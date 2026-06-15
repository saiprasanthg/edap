package com.edap.controller;

import com.edap.dto.ApiResponse;
import com.edap.dto.ComponentRequest;
import com.edap.dto.ComponentResponse;
import com.edap.service.ComponentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/components")
@RequiredArgsConstructor
public class ComponentController {

    private final ComponentService componentService;

    /**
     * GET /api/components
     * List components with optional filters and pagination.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<ComponentResponse>>> list(
            @RequestParam(required = false) String team,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sort) {

        log.info("GET /api/components team={} status={} search={} page={} size={}", team, status, search, page, size);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(sort));
        Page<ComponentResponse> result = componentService.list(team, status, search, pageable);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * GET /api/components/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ComponentResponse>> getById(@PathVariable Long id) {
        log.info("GET /api/components/{}", id);
        return ResponseEntity.ok(ApiResponse.ok(componentService.getById(id)));
    }

    /**
     * POST /api/components
     * Requires ENGINEER or ADMIN role.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER')")
    public ResponseEntity<ApiResponse<ComponentResponse>> create(@Valid @RequestBody ComponentRequest request) {
        log.info("POST /api/components name={}", request.getName());
        ComponentResponse created = componentService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Component created", created));
    }

    /**
     * PUT /api/components/{id}
     * Requires ENGINEER or ADMIN role.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER')")
    public ResponseEntity<ApiResponse<ComponentResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ComponentRequest request) {
        log.info("PUT /api/components/{}", id);
        return ResponseEntity.ok(ApiResponse.ok("Component updated", componentService.update(id, request)));
    }

    /**
     * DELETE /api/components/{id}
     * Requires ADMIN role.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        log.info("DELETE /api/components/{}", id);
        componentService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Component deleted", null));
    }
}
