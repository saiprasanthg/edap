package com.edap.controller;

import com.edap.dto.ApiResponse;
import com.edap.dto.DependencyGraphResponse;
import com.edap.dto.DependencyRequest;
import com.edap.dto.DependencyResponse;
import com.edap.service.DependencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/dependencies")
@RequiredArgsConstructor
public class DependencyController {

    private final DependencyService dependencyService;

    /**
     * POST /api/dependencies
     * Create a directed dependency edge in Neo4j.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER')")
    public ResponseEntity<ApiResponse<DependencyResponse>> create(@Valid @RequestBody DependencyRequest request) {
        log.info("POST /api/dependencies {} -> {}", request.getSourceComponentId(), request.getTargetComponentId());
        DependencyResponse created = dependencyService.createDependency(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Dependency created", created));
    }

    /**
     * GET /api/dependencies/graph/{componentId}?depth=3
     * Returns the dependency sub-graph from the given root component.
     */
    @GetMapping("/graph/{componentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<DependencyGraphResponse>> getDependencyGraph(
            @PathVariable Long componentId,
            @RequestParam(defaultValue = "3") int depth) {
        log.info("GET /api/dependencies/graph/{} depth={}", componentId, depth);
        if (depth < 1 || depth > 10) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Depth must be between 1 and 10"));
        }
        DependencyGraphResponse graph = dependencyService.getDependencyGraph(componentId, depth);
        return ResponseEntity.ok(ApiResponse.ok(graph));
    }

    /**
     * GET /api/dependencies/{componentId}/outgoing
     * Direct outgoing dependencies of a component.
     */
    @GetMapping("/{componentId}/outgoing")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DependencyResponse>>> getOutgoing(@PathVariable Long componentId) {
        log.info("GET /api/dependencies/{}/outgoing", componentId);
        return ResponseEntity.ok(ApiResponse.ok(dependencyService.getDirectDependencies(componentId)));
    }

    /**
     * GET /api/dependencies/{componentId}/incoming
     * Components that depend on this component (reverse edges).
     */
    @GetMapping("/{componentId}/incoming")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DependencyResponse>>> getIncoming(@PathVariable Long componentId) {
        log.info("GET /api/dependencies/{}/incoming", componentId);
        return ResponseEntity.ok(ApiResponse.ok(dependencyService.getDependents(componentId)));
    }
}
