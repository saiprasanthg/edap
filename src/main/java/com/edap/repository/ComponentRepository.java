package com.edap.repository;

import com.edap.entity.Component;
import com.edap.entity.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComponentRepository extends JpaRepository<Component, Long> {

    Optional<Component> findByName(String name);

    Page<Component> findByTeam(Team team, Pageable pageable);

    Page<Component> findByStatus(String status, Pageable pageable);

    Page<Component> findByTeamAndStatus(Team team, String status, Pageable pageable);

    List<Component> findByType(String type);

    @Query("SELECT c FROM Component c LEFT JOIN FETCH c.team WHERE c.id = :id")
    Optional<Component> findByIdWithTeam(@Param("id") Long id);

    @Query("SELECT c FROM Component c LEFT JOIN FETCH c.team WHERE c.team.name = :teamName")
    List<Component> findByTeamName(@Param("teamName") String teamName);

    @Query("SELECT c FROM Component c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Component> search(@Param("query") String query, Pageable pageable);

    long countByStatus(String status);
}
