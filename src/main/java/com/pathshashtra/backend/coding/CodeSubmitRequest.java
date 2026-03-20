package com.pathshashtra.backend.coding;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CodeSubmitRequest {
    private Long problemId;
    private String code;
    private String language;
}
