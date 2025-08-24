package com.sapphireims.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapphireims.engine.dsl.WorkflowSpec;
import com.sapphireims.engine.repo.WfTaskRepo;
import com.sapphireims.engine.service.DefinitionService;
import com.sapphireims.engine.service.EngineService;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EngineServiceTest {
    @Autowired
    DefinitionService definitionService;
    @Autowired
    EngineService engineService;
    @Autowired
    WfTaskRepo taskRepo;
    @Autowired
    ObjectMapper mapper;

    @Test
    void startWorkflowCreatesTask() throws Exception {
        String json = """
{"key":"ticket_flow","name":"Ticket Workflow","version":1,"nodes":[{"id":"start","type":"startEvent","outgoing":[{"to":"n1"}]},{"id":"n1","type":"userTask","name":"New","assignee":"${createBy}","outgoing":[{"to":"n2"}]},{"id":"n2","type":"userTask","name":"Work in progress","assignee":"${technician}","outgoing":[{"to":"e1"}]},{"id":"e1","type":"endEvent"}]}""";
        WorkflowSpec spec = mapper.readValue(json, WorkflowSpec.class);
        definitionService.publish(spec, "t1");
        var inst = engineService.start("ticket_flow", "BK1", Map.of("createBy","alice","technician","bob"), "t1");
        var tasks = taskRepo.findReadyForUserOrGroup("alice", "", "t1");
        Assertions.assertEquals(1, tasks.size());
        Assertions.assertEquals("New", tasks.get(0).getName());
    }

    @Test
    void exclusiveGatewayChoosesBranch() throws Exception {
        String json = """
{"key":"prio_flow","name":"Priority","version":1,"nodes":[
{"id":"start","type":"startEvent","outgoing":[{"to":"n1"}]},
{"id":"n1","type":"userTask","name":"Check","assignee":"${user}","outgoing":[{"to":"g1"}]},
{"id":"g1","type":"exclusiveGateway","outgoing":[
 {"to":"n2","condition":"${ticket.priority=='p1'}"},
 {"to":"n3","condition":"${ticket.priority=='p2'}"}
]},
{"id":"n2","type":"userTask","name":"P1","outgoing":[{"to":"e1"}]},
{"id":"n3","type":"userTask","name":"P2","outgoing":[{"to":"e1"}]},
{"id":"e1","type":"endEvent"}
]}
""";
        WorkflowSpec spec = mapper.readValue(json, WorkflowSpec.class);
        definitionService.publish(spec, "t1");
        var vars = Map.of("user","alice","ticket", Map.of("priority","p1"));
        var inst = engineService.start("prio_flow","BK1", vars, "t1");
        var tasks = taskRepo.findReadyForUserOrGroup("alice","","t1");
        engineService.completeTask(tasks.get(0).getId(), Map.of(), "t1");
        var next = taskRepo.findReadyForUserOrGroup("alice","","t1");
        Assertions.assertEquals("P1", next.get(0).getName());
    }
}
