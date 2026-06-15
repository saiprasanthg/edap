package com.edap.controller;

import com.edap.dto.ApiResponse;
import com.edap.dto.TeamRequest;
import com.edap.dto.TeamResponse;
import com.edap.service.TeamService;
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
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<TeamResponse>>> list(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                teamService.list(PageRequest.of(page, size, Sort.by("name")))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TeamResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(teamService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER')")
    public ResponseEntity<ApiResponse<TeamResponse>> create(@Valid @RequestBody TeamRequest request) {
        log.info("POST /api/teams name={}", request.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Team created", teamService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ENGINEER')")
    public ResponseEntity<ApiResponse<TeamResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody TeamRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Team updated", teamService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        teamService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Team deleted", null));
    }
}
