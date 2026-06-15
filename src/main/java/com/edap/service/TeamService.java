package com.edap.service;

import com.edap.dto.TeamRequest;
import com.edap.dto.TeamResponse;
import com.edap.entity.Team;
import com.edap.exception.ResourceAlreadyExistsException;
import com.edap.exception.ResourceNotFoundException;
import com.edap.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;

    @Transactional(readOnly = true)
    public Page<TeamResponse> list(Pageable pageable) {
        return teamRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TeamResponse getById(Long id) {
        return toResponse(teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", id)));
    }

    @Transactional
    public TeamResponse create(TeamRequest request) {
        log.info("Creating team name={}", request.getName());
        if (teamRepository.existsByName(request.getName())) {
            throw new ResourceAlreadyExistsException("Team", "name", request.getName());
        }
        Team team = Team.builder()
                .name(request.getName())
                .description(request.getDescription())
                .slackChannel(request.getSlackChannel())
                .email(request.getEmail())
                .build();
        return toResponse(teamRepository.save(team));
    }

    @Transactional
    public TeamResponse update(Long id, TeamRequest request) {
        log.info("Updating team id={}", id);
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", id));

        if (!team.getName().equals(request.getName()) && teamRepository.existsByName(request.getName())) {
            throw new ResourceAlreadyExistsException("Team", "name", request.getName());
        }
        team.setName(request.getName());
        team.setDescription(request.getDescription());
        team.setSlackChannel(request.getSlackChannel());
        team.setEmail(request.getEmail());
        return toResponse(teamRepository.save(team));
    }

    @Transactional
    public void delete(Long id) {
        log.info("Deleting team id={}", id);
        if (!teamRepository.existsById(id)) {
            throw new ResourceNotFoundException("Team", "id", id);
        }
        teamRepository.deleteById(id);
    }

    private TeamResponse toResponse(Team t) {
        return TeamResponse.builder()
                .id(t.getId())
                .name(t.getName())
                .description(t.getDescription())
                .slackChannel(t.getSlackChannel())
                .email(t.getEmail())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
