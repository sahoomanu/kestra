package com.sapphireims.engine.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapphireims.engine.domain.*;
import com.sapphireims.engine.dsl.NodeSpec;
import com.sapphireims.engine.dsl.WorkflowSpec;
import com.sapphireims.engine.dsl.Outgoing;
import com.sapphireims.engine.repo.*;
import com.sapphireims.engine.listener.TaskListener;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.camunda.spin.Spin;
import org.camunda.spin.json.SpinJsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EngineService {
    private final WfDefinitionRepo definitionRepo;
    private final WfInstanceRepo instanceRepo;
    private final WfTaskRepo taskRepo;
    private final WfAuditRepo auditRepo;
    private final ObjectMapper objectMapper;
    private final TaskScheduler taskScheduler;
    private final List<TaskListener> listeners;
    private final ConditionEvaluator evaluator = new ConditionEvaluator();

    @Transactional
    public WfInstance start(String defKey, String businessKey, Map<String,Object> vars, String tenantId) {
        WfDefinition def = definitionRepo.findFirstByDefKeyAndTenantIdOrderByVersionDesc(defKey, tenantId)
                .orElseThrow();
        try {
            WorkflowSpec spec = objectMapper.readValue(def.getSpecJson(), WorkflowSpec.class);
            WfInstance inst = new WfInstance();
            inst.setDefinition(def);
            inst.setBusinessKey(businessKey);
            inst.setTenantId(tenantId);
            inst.setState("RUNNING");
            inst.setVariablesJson(objectMapper.writeValueAsString(vars));
            instanceRepo.save(inst);

            Map<String, NodeSpec> nodeMap = mapNodes(spec);
            NodeSpec start = spec.getNodes().stream()
                    .filter(n -> "startEvent".equals(n.getType()))
                    .findFirst().orElseThrow();
            String firstId = start.getOutgoing().get(0).getTo();
            createNode(nodeMap.get(firstId), inst, vars);
            return inst;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void claimTask(Long id, String user, String tenantId) {
        WfTask task = taskRepo.findById(id).orElseThrow();
        if (!task.getTenantId().equals(tenantId)) throw new IllegalArgumentException("Wrong tenant");
        task.setAssignee(user);
        taskRepo.save(task);
    }

    @Transactional
    public void completeTask(Long id, Map<String,Object> output, String tenantId) {
        try {
            WfTask task = taskRepo.findById(id).orElseThrow();
            if (!task.getTenantId().equals(tenantId)) throw new IllegalArgumentException("Wrong tenant");
            task.setStatus("COMPLETED");
            task.setOutputJson(objectMapper.writeValueAsString(output));
            task.setCompletedAt(java.time.Instant.now());
            taskRepo.save(task);
            listeners.forEach(l -> l.onComplete(task, output));

            WfInstance inst = task.getWfInstance();
            inst.setCurrentNodeId(task.getNodeId());
            instanceRepo.save(inst);

            WfDefinition def = inst.getDefinition();
            WorkflowSpec spec = objectMapper.readValue(def.getSpecJson(), WorkflowSpec.class);
            Map<String, NodeSpec> nodeMap = mapNodes(spec);
            NodeSpec current = nodeMap.get(task.getNodeId());
            SpinJsonNode spinVars = Spin.JSON(inst.getVariablesJson());
            Map<String,Object> vars = spinVars.map();
            for (Outgoing out : current.getOutgoing()) {
                if (evaluator.evaluate(out.getCondition(), vars)) {
                    NodeSpec next = nodeMap.get(out.getTo());
                    handleNode(next, inst, vars, nodeMap);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleNode(NodeSpec node, WfInstance inst, Map<String,Object> vars, Map<String,NodeSpec> nodeMap) {
        switch (node.getType()) {
            case "userTask" -> createNode(node, inst, vars);
            case "exclusiveGateway" -> {
                for (Outgoing out : node.getOutgoing()) {
                    if (evaluator.evaluate(out.getCondition(), vars)) {
                        handleNode(nodeMap.get(out.getTo()), inst, vars, nodeMap);
                        break;
                    }
                }
            }
            case "parallelGateway" -> {
                for (Outgoing out : node.getOutgoing()) {
                    handleNode(nodeMap.get(out.getTo()), inst, vars, nodeMap);
                }
            }
            case "inclusiveGateway" -> {
                for (Outgoing out : node.getOutgoing()) {
                    if (evaluator.evaluate(out.getCondition(), vars)) {
                        handleNode(nodeMap.get(out.getTo()), inst, vars, nodeMap);
                    }
                }
            }
            case "endEvent" -> {
                inst.setState("COMPLETED");
                instanceRepo.save(inst);
            }
            default -> throw new IllegalArgumentException("Unsupported node type " + node.getType());
        }
    }

    private void createNode(NodeSpec node, WfInstance inst, Map<String,Object> vars) {
        WfTask task = new WfTask();
        task.setWfInstance(inst);
        task.setNodeId(node.getId());
        task.setTenantId(inst.getTenantId());
        task.setName(node.getName());
        task.setAssignee(evaluator.render(node.getAssignee(), vars));
        task.setCandidateGroups(evaluator.render(node.getCandidateGroups(), vars));
        task.setStatus("READY");
        taskRepo.save(task);
        taskScheduler.schedule(task);
        listeners.forEach(l -> l.onStart(task));
    }

    private Map<String, NodeSpec> mapNodes(WorkflowSpec spec) {
        Map<String, NodeSpec> nodeMap = new HashMap<>();
        for (NodeSpec n : spec.getNodes()) {
            nodeMap.put(n.getId(), n);
        }
        return nodeMap;
    }

    
}
