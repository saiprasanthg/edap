package com.edap.entity;

import lombok.*;
import org.springframework.data.neo4j.core.schema.*;

/**
 * Neo4j relationship representing a directional dependency between two ComponentNodes.
 *
 * Types: DEPENDS_ON | CALLS | OWNS
 */
@RelationshipProperties
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DependencyRelationship {

    @RelationshipId
    private Long id;

    /** The relationship type label stored as a property for easy querying */
    @Property("type")
    @Builder.Default
    private String type = "DEPENDS_ON";

    /**
     * Numeric weight of the dependency, e.g. call-rate or criticality score.
     * Range 0.0 – 1.0 where 1.0 = maximum weight.
     */
    @Property("weight")
    @Builder.Default
    private Double weight = 1.0;

    @Property("description")
    private String description;

    @TargetNode
    private ComponentNode target;
}
