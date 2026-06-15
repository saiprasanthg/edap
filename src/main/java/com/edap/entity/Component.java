package com.edap.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "components", indexes = {
    @Index(name = "idx_component_name", columnList = "name"),
    @Index(name = "idx_component_team", columnList = "team_id"),
    @Index(name = "idx_component_status", columnList = "status"),
    @Index(name = "idx_component_type", columnList = "type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Component {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String name;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String type;   // e.g. SERVICE, LIBRARY, DATABASE, QUEUE, GATEWAY

    @Size(max = 200)
    @Column(length = 200)
    private String owner;  // individual owner email / username

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "ACTIVE";  // ACTIVE, DEPRECATED, RETIRED, EXPERIMENTAL

    @Size(max = 1000)
    @Column(length = 1000)
    private String description;

    @Size(max = 500)
    @Column(name = "repository_url", length = 500)
    private String repositoryUrl;

    /**
     * Flexible key-value metadata stored as JSON in MySQL.
     * Examples: {"language": "Java", "runtime": "JVM17", "sla": "99.9%"}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
