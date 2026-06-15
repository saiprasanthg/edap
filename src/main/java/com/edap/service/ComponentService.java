package com.edap.service;

import com.edap.dto.ComponentRequest;
import com.edap.dto.ComponentResponse;
import com.edap.entity.Component;
import com.edap.entity.ComponentNode;
import com.edap.entity.Team;
import com.edap.exception.ResourceNotFoundException;
import com.edap.exception.ResourceAlreadyExistsException;
import com.edap.repository.ComponentNodeRepository;
import com.edap.repository.ComponentRepository;
import com.edap.repository.TeamRepository;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComponentService {

    private final ComponentRepository componentRepository;
    private final ComponentNodeRepository componentNodeRepository;
    private final TeamRepository teamRepository;
    private final Counter componentLookupCounter;
    private final AtomicLong dependencyGraphNodeCount;

    @Transactional(readOnly = true)
    public ComponentResponse getById(Long id) {
        log.info("Fetching component id={}", id);
        componentLookupCounter.increment();
        Component c = componentRepository.findByIdWithTeam(id)
                .orElseThrow(() -> new ResourceNotFoundException("Component", "id", id));
        return toResponse(c);
    }

    @Transactional(readOnly = true)
    public Page<ComponentResponse> list(String teamName, String status, String search, Pageable pageable) {
        log.debug("Listing components: team={}, status={}, search={}", teamName, status, search);
        componentLookupCounter.increment();

        if (search != null && !search.isBlank()) {
            return componentRepository.search(search, pageable).map(this::toResponse);
        }
        if (teamName != null && status != null) {
            Team team = teamRepository.findByName(teamName)
                    .orElseThrow(() -> new ResourceNotFoundException("Team", "name", teamName));
            return componentRepository.findByTeamAndStatus(team, status, pageable).map(this::toResponse);
        }
        if (teamName != null) {
            Team team = teamRepository.findByName(teamName)
                    .orElseThrow(() -> new ResourceNotFoundException("Team", "name", teamName));
            return componentRepository.findByTeam(team, pageable).map(this::toResponse);
        }
        if (status != null) {
            return componentRepository.findByStatus(status, pageable).map(this::toResponse);
        }
        return componentRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public ComponentResponse create(ComponentRequest request) {
        log.info("Creating component name={}", request.getName());

        if (componentRepository.findByName(request.getName()).isPresent()) {
            throw new ResourceAlreadyExistsException("Component", "name", request.getName());
        }

        Team team = null;
        if (request.getTeamId() != null) {
            team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team", "id", request.getTeamId()));
        }

        Component component = Component.builder()
                .name(request.getName())
                .type(request.getType())
                .owner(request.getOwner())
                .team(team)
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .description(request.getDescription())
                .repositoryUrl(request.getRepositoryUrl())
                .metadata(request.getMetadata())
                .build();

        component = componentRepository.save(component);
        log.info("Saved MySQL component id={}", component.getId());

        // Mirror into Neo4j
        ComponentNode node = ComponentNode.builder()
                .componentId(component.getId())
                .name(component.getName())
                .type(component.getType())
                .status(component.getStatus())
                .teamName(team != null ? team.getName() : null)
                .build();
        componentNodeRepository.save(node);
        dependencyGraphNodeCount.set(componentNodeRepository.countAllNodes());
        log.info("Mirrored component id={} into Neo4j graph", component.getId());

        return toResponse(component);
    }

    @Transactional
    public ComponentResponse update(Long id, ComponentRequest request) {
        log.info("Updating component id={}", id);
        Component component = componentRepository.findByIdWithTeam(id)
                .orElseThrow(() -> new ResourceNotFoundException("Component", "id", id));

        // Name uniqueness check (skip if name unchanged)
        if (!component.getName().equals(request.getName()) &&
            componentRepository.findByName(request.getName()).isPresent()) {
            throw new ResourceAlreadyExistsException("Component", "name", request.getName());
        }

        Team team = null;
        if (request.getTeamId() != null) {
            team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team", "id", request.getTeamId()));
        }

        component.setName(request.getName());
        component.setType(request.getType());
        component.setOwner(request.getOwner());
        component.setTeam(team);
        component.setStatus(request.getStatus() != null ? request.getStatus() : component.getStatus());
        component.setDescription(request.getDescription());
        component.setRepositoryUrl(request.getRepositoryUrl());
        component.setMetadata(request.getMetadata());

        component = componentRepository.save(component);

        // Sync Neo4j node
        Team finalTeam = team;
        componentNodeRepository.findByComponentId(id).ifPresent(node -> {
            node.setName(component.getName());
            node.setType(component.getType());
            node.setStatus(component.getStatus());
            node.setTeamName(finalTeam != null ? finalTeam.getName() : null);
            componentNodeRepository.save(node);
        });

        return toResponse(component);
    }

    @Transactional
    public void delete(Long id) {
        log.info("Deleting component id={}", id);
        if (!componentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Component", "id", id);
        }
        componentRepository.deleteById(id);
        componentNodeRepository.findByComponentId(id).ifPresent(node -> {
            componentNodeRepository.delete(node);
            dependencyGraphNodeCount.set(componentNodeRepository.countAllNodes());
            log.info("Removed Neo4j node for component id={}", id);
        });
    }

    public ComponentResponse toResponse(Component c) {
        return ComponentResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .type(c.getType())
                .owner(c.getOwner())
                .teamId(c.getTeam() != null ? c.getTeam().getId() : null)
                .teamName(c.getTeam() != null ? c.getTeam().getName() : null)
                .status(c.getStatus())
                .description(c.getDescription())
                .repositoryUrl(c.getRepositoryUrl())
                .metadata(c.getMetadata())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
