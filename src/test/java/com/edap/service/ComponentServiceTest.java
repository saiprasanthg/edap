package com.edap.service;

import com.edap.dto.ComponentRequest;
import com.edap.dto.ComponentResponse;
import com.edap.entity.Component;
import com.edap.entity.ComponentNode;
import com.edap.entity.Team;
import com.edap.exception.ResourceAlreadyExistsException;
import com.edap.exception.ResourceNotFoundException;
import com.edap.repository.ComponentNodeRepository;
import com.edap.repository.ComponentRepository;
import com.edap.repository.TeamRepository;
import io.micrometer.core.instrument.Counter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ComponentService unit tests")
class ComponentServiceTest {

    @Mock ComponentRepository componentRepository;
    @Mock ComponentNodeRepository componentNodeRepository;
    @Mock TeamRepository teamRepository;
    @Mock Counter componentLookupCounter;

    // AtomicLong is not a mockable bean easily — inject a real one
    private final AtomicLong dependencyGraphNodeCount = new AtomicLong(0L);

    ComponentService componentService;

    private Team team;
    private Component component;

    @BeforeEach
    void setUp() {
        componentService = new ComponentService(
                componentRepository,
                componentNodeRepository,
                teamRepository,
                componentLookupCounter,
                dependencyGraphNodeCount);

        team = Team.builder().id(1L).name("platform").build();
        component = Component.builder()
                .id(10L)
                .name("user-service")
                .type("SERVICE")
                .owner("alice@edap")
                .team(team)
                .status("ACTIVE")
                .metadata(Map.of("lang", "Java"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    // ── getById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById returns ComponentResponse when component exists")
    void getById_existingComponent_returnsResponse() {
        given(componentRepository.findByIdWithTeam(10L)).willReturn(Optional.of(component));

        ComponentResponse response = componentService.getById(10L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("user-service");
        assertThat(response.getTeamName()).isEqualTo("platform");
        verify(componentLookupCounter, times(1)).increment();
    }

    @Test
    @DisplayName("getById throws ResourceNotFoundException when component is missing")
    void getById_missingComponent_throwsNotFound() {
        given(componentRepository.findByIdWithTeam(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> componentService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── create ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create persists component and mirrors to Neo4j")
    void create_validRequest_savesComponentAndNode() {
        ComponentRequest request = new ComponentRequest();
        request.setName("new-service");
        request.setType("SERVICE");
        request.setOwner("bob@edap");
        request.setTeamId(1L);
        request.setStatus("ACTIVE");
        request.setMetadata(Map.of());

        given(componentRepository.findByName("new-service")).willReturn(Optional.empty());
        given(teamRepository.findById(1L)).willReturn(Optional.of(team));
        given(componentRepository.save(any(Component.class))).willReturn(
                Component.builder().id(11L).name("new-service").type("SERVICE")
                        .team(team).status("ACTIVE").metadata(Map.of())
                        .createdAt(Instant.now()).updatedAt(Instant.now()).build());
        given(componentNodeRepository.save(any(ComponentNode.class))).willReturn(
                ComponentNode.builder().componentId(11L).build());
        given(componentNodeRepository.countAllNodes()).willReturn(1L);

        ComponentResponse response = componentService.create(request);

        assertThat(response.getId()).isEqualTo(11L);
        assertThat(response.getName()).isEqualTo("new-service");

        // Verify Neo4j node was persisted
        ArgumentCaptor<ComponentNode> nodeCaptor = ArgumentCaptor.forClass(ComponentNode.class);
        verify(componentNodeRepository, times(1)).save(nodeCaptor.capture());
        assertThat(nodeCaptor.getValue().getComponentId()).isEqualTo(11L);
        assertThat(nodeCaptor.getValue().getTeamName()).isEqualTo("platform");
    }

    @Test
    @DisplayName("create throws ResourceAlreadyExistsException when name is taken")
    void create_duplicateName_throwsConflict() {
        ComponentRequest request = new ComponentRequest();
        request.setName("user-service");
        request.setType("SERVICE");

        given(componentRepository.findByName("user-service")).willReturn(Optional.of(component));

        assertThatThrownBy(() -> componentService.create(request))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("user-service");
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete removes component from MySQL and Neo4j")
    void delete_existingComponent_removesFromBothStores() {
        given(componentRepository.existsById(10L)).willReturn(true);
        given(componentNodeRepository.findByComponentId(10L)).willReturn(
                Optional.of(ComponentNode.builder().componentId(10L).build()));
        given(componentNodeRepository.countAllNodes()).willReturn(0L);

        componentService.delete(10L);

        verify(componentRepository).deleteById(10L);
        verify(componentNodeRepository).delete(any(ComponentNode.class));
    }

    @Test
    @DisplayName("delete throws ResourceNotFoundException when component missing")
    void delete_missingComponent_throwsNotFound() {
        given(componentRepository.existsById(42L)).willReturn(false);

        assertThatThrownBy(() -> componentService.delete(42L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(componentRepository, never()).deleteById(any());
    }
}
