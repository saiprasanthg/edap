package com.edap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ComponentRequest {

    @NotBlank(message = "Component name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    @NotBlank(message = "Component type is required")
    @Pattern(regexp = "SERVICE|LIBRARY|DATABASE|QUEUE|GATEWAY|CACHE|FRONTEND|WORKER",
             message = "Type must be one of: SERVICE, LIBRARY, DATABASE, QUEUE, GATEWAY, CACHE, FRONTEND, WORKER")
    private String type;

    @Size(max = 200)
    private String owner;

    private Long teamId;

    @Pattern(regexp = "ACTIVE|DEPRECATED|RETIRED|EXPERIMENTAL",
             message = "Status must be one of: ACTIVE, DEPRECATED, RETIRED, EXPERIMENTAL")
    private String status = "ACTIVE";

    @Size(max = 1000)
    private String description;

    @Size(max = 500)
    private String repositoryUrl;

    private Map<String, String> metadata = new HashMap<>();
}
