package com.edap.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DependencyGraphResponse {
    private List<ComponentResponse> nodes;
    private List<DependencyResponse> edges;
    private int totalNodes;
    private int totalEdges;
}
