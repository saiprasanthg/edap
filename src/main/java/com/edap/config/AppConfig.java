package com.edap.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Configuration
public class AppConfig {

    /**
     * Tracks total component lookup requests (REST + gRPC combined).
     * Exposed as: edap_component_lookup_total
     */
    @Primary
    @Bean("transactionManager")
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    @Bean
    public Counter componentLookupCounter(MeterRegistry registry) {
        return Counter.builder("edap_component_lookup_total")
                .description("Total number of component lookup requests")
                .tag("source", "all")
                .register(registry);
    }

    /**
     * Holds the current count of Neo4j graph nodes.
     * Exposed as: edap_dependency_graph_nodes
     */
    @Bean
    public AtomicLong dependencyGraphNodeCount() {
        return new AtomicLong(0);
    }

    @Bean
    public Gauge dependencyGraphNodesGauge(MeterRegistry registry,
                                           AtomicLong dependencyGraphNodeCount) {
        return Gauge.builder("edap_dependency_graph_nodes", dependencyGraphNodeCount, AtomicLong::doubleValue)
                .description("Current number of component nodes in the Neo4j dependency graph")
                .register(registry);
    }
}
