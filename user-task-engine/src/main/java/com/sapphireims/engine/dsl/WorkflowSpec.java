package com.sapphireims.engine.dsl;

import java.util.List;
import lombok.Data;

@Data
public class WorkflowSpec {
    private String key;
    private String name;
    private int version;
    private List<NodeSpec> nodes;
}
