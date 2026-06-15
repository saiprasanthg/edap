package com.edap.repository;

import com.edap.entity.ComponentNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComponentNodeRepository extends Neo4jRepository<ComponentNode, Long> {

    Optional<ComponentNode> findByComponentId(Long componentId);

    List<ComponentNode> findByType(String type);

    List<ComponentNode> findByTeamName(String teamName);

    /**
     * BFS up to {@code depth} hops from the given root node.
     * Returns all nodes reachable within the specified hop count.
     */
    @Query("MATCH path = (root:Component {componentId: $componentId})-[:DEPENDS_ON*0..$depth]->(dep:Component) " +
           "RETURN root, collect(nodes(path)), collect(relationships(path))")
    List<ComponentNode> findDependencyGraph(@Param("componentId") Long componentId,
                                            @Param("depth") int depth);

    /**
     * Return all nodes that directly depend on this component (reverse edges).
     */
    @Query("MATCH (dep:Component)-[:DEPENDS_ON]->(target:Component {componentId: $componentId}) RETURN dep")
    List<ComponentNode> findDependents(@Param("componentId") Long componentId);

    @Query("MATCH (n:Component) RETURN count(n)")
    long countAllNodes();
}
