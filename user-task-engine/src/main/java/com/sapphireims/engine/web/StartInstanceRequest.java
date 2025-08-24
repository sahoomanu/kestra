package com.sapphireims.engine.web;

import java.util.Map;
import lombok.Data;

@Data
public class StartInstanceRequest {
    private String defKey;
    private String businessKey;
    private Map<String,Object> variables;
}
