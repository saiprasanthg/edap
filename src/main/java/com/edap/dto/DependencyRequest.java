package com.edap.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class DependencyRequest {

    @NotNull(message = "Source component ID is required")
    private Long sourceComponentId;

    @NotNull(message = "Target component ID is required")
    private Long targetComponentId;

    @Pattern(regexp = "DEPENDS_ON|CALLS|OWNS",
             message = "Type must be one of: DEPENDS_ON, CALLS, OWNS")
    private String type = "DEPENDS_ON";

    @DecimalMin(value = "0.0") @DecimalMax(value = "1.0")
    private Double weight = 1.0;

    private String description;
}
