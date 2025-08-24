package com.sapphireims.engine.dsl;

import lombok.Data;

@Data
public class Outgoing {
    private String to;
    private String condition; // optional condition for gateways
}
