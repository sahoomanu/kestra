package com.sapphireims.engine.web;

import com.sapphireims.engine.domain.WfInstance;
import com.sapphireims.engine.repo.WfInstanceRepo;
import com.sapphireims.engine.repo.WfTaskRepo;
import com.sapphireims.engine.service.DefinitionService;
import com.sapphireims.engine.service.EngineService;
import com.sapphireims.engine.dsl.WorkflowSpec;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wf")
@RequiredArgsConstructor
public class WorkflowController {
    private final EngineService engine;
    private final DefinitionService defs;
    private final WfTaskRepo taskRepo;
    private final WfInstanceRepo instRepo;

    @PostMapping("/definitions")
    public ResponseEntity<?> publish(@RequestHeader("X-Tenant") String tenant, @RequestBody WorkflowSpec spec){
        return ResponseEntity.ok(defs.publish(spec, tenant));
    }

    @PostMapping("/start")
    public ResponseEntity<?> start(@RequestHeader("X-Tenant") String tenant, @RequestBody StartInstanceRequest req){
        var inst = engine.start(req.getDefKey(), req.getBusinessKey(), req.getVariables(), tenant);
        return ResponseEntity.ok(Map.of("instanceId", inst.getId()));
    }

    @GetMapping("/tasks")
    public List<TaskView> myTasks(@RequestHeader("X-Tenant") String tenant, @RequestParam String user, @RequestParam(required=false) String group){
        var list = taskRepo.findReadyForUserOrGroup(user, group==null?"":group, tenant);
        return list.stream().map(t-> TaskView.builder()
            .id(t.getId()).wfInstanceId(t.getWfInstance().getId()).nodeId(t.getNodeId()).name(t.getName())
            .assignee(t.getAssignee()).candidateGroups(t.getCandidateGroups())
            .dueDate(t.getDueDate()).followUpDate(t.getFollowUpDate()).priority(t.getPriority())
            .status(t.getStatus()).build()).toList();
    }

    @PostMapping("/tasks/{id}/claim")
    public void claim(@RequestHeader("X-Tenant") String tenant, @PathVariable Long id, @RequestParam String user){ engine.claimTask(id, user, tenant); }

    @PostMapping("/tasks/{id}/complete")
    public void complete(@RequestHeader("X-Tenant") String tenant, @PathVariable Long id, @RequestBody CompleteTaskRequest req){ engine.completeTask(id, req.getOutput(), tenant); }

    @GetMapping("/instances/{id}")
    public WfInstance instance(@PathVariable Long id){ return instRepo.findById(id).orElseThrow(); }
}
