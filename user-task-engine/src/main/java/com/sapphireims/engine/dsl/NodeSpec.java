package com.sapphireims.engine.dsl;

import java.util.List;
import lombok.Data;

@Data
public class NodeSpec {
    private String id;
    private String type;
    private String name;
    private String assignee;
    private String candidateGroups;
    private List<Outgoing> outgoing;
}
