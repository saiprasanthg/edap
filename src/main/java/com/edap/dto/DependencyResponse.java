package com.edap.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DependencyResponse {
    private Long id;
    private Long sourceComponentId;
    private String sourceComponentName;
    private Long targetComponentId;
    private String targetComponentName;
    private String type;
    private Double weight;
    private String description;
}
