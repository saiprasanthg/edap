package com.edap.service;

import com.edap.dto.ComponentResponse;
import com.edap.dto.DependencyGraphResponse;
import com.edap.dto.DependencyRequest;
import com.edap.dto.DependencyResponse;
import com.edap.entity.ComponentNode;
import com.edap.entity.DependencyRelationship;
import com.edap.exception.BadRequestException;
import com.edap.exception.ResourceNotFoundException;
import com.edap.repository.ComponentNodeRepository;
import com.edap.repository.ComponentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DependencyService {

    private final ComponentNodeRepository componentNodeRepository;
    private final ComponentRepository componentRepository;
    private final ComponentService componentService;

    @Transactional
    public DependencyResponse createDependency(DependencyRequest request) {
        if (request.getSourceComponentId().equals(request.getTargetComponentId())) {
            throw new BadRequestException("A component cannot depend on itself");
        }

        log.info("Creating dependency: {} -> {} ({})",
                request.getSourceComponentId(), request.getTargetComponentId(), request.getType());

        ComponentNode source = componentNodeRepository.findByComponentId(request.getSourceComponentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ComponentNode", "componentId", request.getSourceComponentId()));

        ComponentNode target = componentNodeRepository.findByComponentId(request.getTargetComponentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ComponentNode", "componentId", request.getTargetComponentId()));

        // Check for duplicate dependency
        boolean alreadyExists = source.getDependencies().stream()
                .anyMatch(dep -> dep.getTarget().getComponentId().equals(request.getTargetComponentId())
                        && dep.getType().equals(request.getType()));
        if (alreadyExists) {
            throw new BadRequestException("Dependency of type " + request.getType() +
                    " already exists between these components");
        }

        DependencyRelationship rel = DependencyRelationship.builder()
                .type(request.getType())
                .weight(request.getWeight())
                .description(request.getDescription())
                .target(target)
                .build();

        source.getDependencies().add(rel);
        componentNodeRepository.save(source);
        log.info("Saved dependency relationship in Neo4j");

        return buildDependencyResponse(source, rel);
    }

    @Transactional(readOnly = true)
    public DependencyGraphResponse getDependencyGraph(Long componentId, int depth) {
        log.info("Building dependency graph for componentId={}, depth={}", componentId, depth);

        if (!componentRepository.existsById(componentId)) {
            throw new ResourceNotFoundException("Component", "id", componentId);
        }

        // Traverse up to depth hops
        List<ComponentNode> reachableNodes = componentNodeRepository.findDependencyGraph(componentId, depth);

        // Collect unique nodes and edges
        Map<Long, ComponentNode> nodeMap = new HashMap<>();
        List<DependencyResponse> edges = new ArrayList<>();

        for (ComponentNode node : reachableNodes) {
            nodeMap.put(node.getComponentId(), node);
            for (DependencyRelationship rel : node.getDependencies()) {
                ComponentNode tgt = rel.getTarget();
                if (tgt != null) {
                    nodeMap.put(tgt.getComponentId(), tgt);
                    edges.add(DependencyResponse.builder()
                            .id(rel.getId())
                            .sourceComponentId(node.getComponentId())
                            .sourceComponentName(node.getName())
                            .targetComponentId(tgt.getComponentId())
                            .targetComponentName(tgt.getName())
                            .type(rel.getType())
                            .weight(rel.getWeight())
                            .description(rel.getDescription())
                            .build());
                }
            }
        }

        List<ComponentResponse> componentResponses = new ArrayList<>();
        for (Long cId : nodeMap.keySet()) {
            componentRepository.findByIdWithTeam(cId).ifPresent(c ->
                componentResponses.add(componentService.toResponse(c)));
        }

        return DependencyGraphResponse.builder()
                .nodes(componentResponses)
                .edges(edges)
                .totalNodes(componentResponses.size())
                .totalEdges(edges.size())
                .build();
    }

    @Transactional(readOnly = true)
    public List<DependencyResponse> getDirectDependencies(Long componentId) {
        ComponentNode source = componentNodeRepository.findByComponentId(componentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ComponentNode", "componentId", componentId));

        List<DependencyResponse> result = new ArrayList<>();
        for (DependencyRelationship rel : source.getDependencies()) {
            result.add(buildDependencyResponse(source, rel));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<DependencyResponse> getDependents(Long componentId) {
        List<ComponentNode> dependents = componentNodeRepository.findDependents(componentId);
        String targetName = componentNodeRepository.findByComponentId(componentId)
                .map(ComponentNode::getName).orElse("unknown");

        List<DependencyResponse> result = new ArrayList<>();
        for (ComponentNode dep : dependents) {
            dep.getDependencies().stream()
                    .filter(r -> r.getTarget() != null &&
                            r.getTarget().getComponentId().equals(componentId))
                    .forEach(r -> result.add(DependencyResponse.builder()
                            .sourceComponentId(dep.getComponentId())
                            .sourceComponentName(dep.getName())
                            .targetComponentId(componentId)
                            .targetComponentName(targetName)
                            .type(r.getType())
                            .weight(r.getWeight())
                            .build()));
        }
        return result;
    }

    private DependencyResponse buildDependencyResponse(ComponentNode source, DependencyRelationship rel) {
        return DependencyResponse.builder()
                .id(rel.getId())
                .sourceComponentId(source.getComponentId())
                .sourceComponentName(source.getName())
                .targetComponentId(rel.getTarget() != null ? rel.getTarget().getComponentId() : null)
                .targetComponentName(rel.getTarget() != null ? rel.getTarget().getName() : null)
                .type(rel.getType())
                .weight(rel.getWeight())
                .description(rel.getDescription())
                .build();
    }
}
