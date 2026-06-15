package com.edap.entity;

import lombok.*;
import org.springframework.data.neo4j.core.schema.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Neo4j graph node representing a Component.
 * The {@code componentId} field links back to the MySQL Component primary key,
 * allowing cross-store joins at the service layer.
 */
@Node("Component")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ComponentNode {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private Long graphId;

    /** Foreign-key back to MySQL components.id */
    @Property("componentId")
    private Long componentId;

    @Property("name")
    private String name;

    @Property("type")
    private String type;

    @Property("status")
    private String status;

    @Property("teamName")
    private String teamName;

    @Relationship(type = "DEPENDS_ON", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private List<DependencyRelationship> dependencies = new ArrayList<>();
}
