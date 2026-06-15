package com.edap.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class TeamResponse {
    private Long id;
    private String name;
    private String description;
    private String slackChannel;
    private String email;
    private Instant createdAt;
    private Instant updatedAt;
}
