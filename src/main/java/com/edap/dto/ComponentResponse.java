package com.edap.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class ComponentResponse {
    private Long id;
    private String name;
    private String type;
    private String owner;
    private Long teamId;
    private String teamName;
    private String status;
    private String description;
    private String repositoryUrl;
    private Map<String, String> metadata;
    private Instant createdAt;
    private Instant updatedAt;
}
