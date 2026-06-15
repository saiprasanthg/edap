package com.edap.controller;

import com.edap.dto.ComponentRequest;
import com.edap.dto.ComponentResponse;
import com.edap.exception.GlobalExceptionHandler;
import com.edap.exception.ResourceNotFoundException;
import com.edap.service.ComponentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Standalone MockMvc tests for ComponentController.
 * No Spring context is loaded — faster, no auth filter needed.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ComponentController MockMvc tests")
class ComponentControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ComponentService componentService;

    @InjectMocks
    private ComponentController componentController;

    private ComponentResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(componentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper().findAndRegisterModules();

        sampleResponse = ComponentResponse.builder()
                .id(1L)
                .name("user-service")
                .type("SERVICE")
                .owner("alice@edap")
                .teamId(1L)
                .teamName("platform")
                .status("ACTIVE")
                .metadata(Map.of("lang", "Java"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("GET /api/components returns paginated component list")
    void list_returnsPagedComponents() throws Exception {
        given(componentService.list(isNull(), isNull(), isNull(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(sampleResponse)));

        mockMvc.perform(get("/api/components").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("user-service"))
                .andExpect(jsonPath("$.data.content[0].type").value("SERVICE"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/components/{id} returns component")
    void getById_returnsComponent() throws Exception {
        given(componentService.getById(1L)).willReturn(sampleResponse);

        mockMvc.perform(get("/api/components/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.teamName").value("platform"));
    }

    @Test
    @DisplayName("GET /api/components/{id} returns 404 for unknown id")
    void getById_unknownId_returns404() throws Exception {
        given(componentService.getById(99L))
                .willThrow(new ResourceNotFoundException("Component", "id", 99L));

        mockMvc.perform(get("/api/components/99").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error", containsString("99")));
    }

    @Test
    @DisplayName("POST /api/components creates component and returns 201")
    void create_validRequest_returns201() throws Exception {
        ComponentRequest request = new ComponentRequest();
        request.setName("new-service");
        request.setType("SERVICE");

        ComponentResponse created = ComponentResponse.builder()
                .id(2L).name("new-service").type("SERVICE").status("ACTIVE")
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();

        given(componentService.create(any(ComponentRequest.class))).willReturn(created);

        mockMvc.perform(post("/api/components")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.name").value("new-service"));
    }

    @Test
    @DisplayName("POST /api/components returns 400 for missing required fields")
    void create_missingName_returns400() throws Exception {
        ComponentRequest request = new ComponentRequest();
        // name is blank, type is blank

        mockMvc.perform(post("/api/components")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("DELETE /api/components/{id} returns 200")
    void delete_existingComponent_returns200() throws Exception {
        mockMvc.perform(delete("/api/components/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(componentService).delete(1L);
    }
}
