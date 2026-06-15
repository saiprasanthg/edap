# Engineering Data Access Platform (EDAP)

A production-grade backend platform for managing engineering component metadata and dependency graphs. Built as a portfolio/showcase project demonstrating enterprise Spring Boot patterns.

---

## Architecture Overview

```
┌────────────────────────────────────────────────────────────┐
│                     Clients                                 │
│   REST (HTTP/JSON)            gRPC (HTTP/2 + Protobuf)     │
└───────────────┬──────────────────────────┬─────────────────┘
                │  :8080                   │  :9090
        ┌───────▼──────────────────────────▼───────┐
        │         Spring Boot Application            │
        │                                            │
        │  ┌──────────────┐   ┌───────────────────┐ │
        │  │  REST Layer  │   │   gRPC Service    │ │
        │  │ Controllers  │   │ EntityGrpcService │ │
        │  └──────┬───────┘   └────────┬──────────┘ │
        │         └──────────┬──────────┘            │
        │              ┌─────▼──────┐                │
        │              │  Services  │                 │
        │              └─────┬──────┘                │
        │         ┌──────────┼──────────┐            │
        │    ┌────▼────┐           ┌────▼──────┐     │
        │    │  MySQL  │           │   Neo4j   │     │
        │    │  (JPA)  │           │  (SDN5)   │     │
        │    └─────────┘           └───────────┘     │
        └─────────────────────────────────────────────┘
                         │
                  ┌──────▼───────┐
                  │  Prometheus  │  :9091
                  └──────────────┘
```

### Key Design Decisions

| Concern | Choice | Rationale |
|---------|--------|-----------|
| Structured entity data | MySQL + JPA/Hibernate | Relational integrity, complex joins |
| Dependency graphs | Neo4j + Spring Data Neo4j | Graph traversal (BFS, DFS) is native |
| Auth | JWT (stateless) | Horizontally scalable, no session store |
| RBAC | Spring Security `@PreAuthorize` | Declarative, easy to audit |
| Internal RPC | gRPC + Protobuf | Low latency, strongly typed contracts |
| Metrics | Micrometer + Prometheus | Industry standard, Grafana-ready |
| Migrations | Flyway | Versioned, reproducible schema changes |

---

## Running Locally

### Prerequisites
- Java 17+
- Maven 3.9+
- Docker & Docker Compose

### Option A — Docker Compose (recommended)

```bash
cd edap/

# Build and start all services (MySQL, Neo4j, app, Prometheus)
docker-compose up --build

# Tail logs for the app only
docker-compose logs -f app
```

Services exposed:

| Service    | URL                                      |
|------------|------------------------------------------|
| REST API   | http://localhost:8080                    |
| gRPC       | localhost:9090                           |
| Neo4j UI   | http://localhost:7474 (neo4j/neo4jpass)  |
| Prometheus | http://localhost:9091                    |
| Actuator   | http://localhost:8080/actuator           |

### Option B — Local Maven

Start MySQL and Neo4j via Docker first:

```bash
docker run -d --name edap-mysql \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=edap_db \
  -e MYSQL_USER=edap_user \
  -e MYSQL_PASSWORD=edap_pass \
  -p 3306:3306 mysql:8.0

docker run -d --name edap-neo4j \
  -e NEO4J_AUTH=neo4j/neo4jpass \
  -p 7474:7474 -p 7687:7687 neo4j:5
```

Then run the app:

```bash
mvn spring-boot:run
```

---

## Seed Credentials

| Username   | Password      | Roles                             |
|------------|---------------|-----------------------------------|
| `admin`    | `Admin@123`   | ROLE_ADMIN, ROLE_ENGINEER, ROLE_VIEWER |
| `engineer` | `Engineer@123`| ROLE_ENGINEER, ROLE_VIEWER         |
| `viewer`   | `Viewer@123`  | ROLE_VIEWER                        |

---

## REST API Examples

### Authentication

```bash
# Login — get JWT
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@123"}' \
  | jq -r '.data.accessToken')

echo "Token: $TOKEN"

# Register a new user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newengineer",
    "email": "newengineer@example.com",
    "password": "Secure@1234",
    "roles": ["ROLE_ENGINEER", "ROLE_VIEWER"]
  }'
```

### Components

```bash
# List all components (paginated)
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/components?page=0&size=10"

# Filter by team and status
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/components?team=platform&status=ACTIVE"

# Full-text search
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/components?search=gateway"

# Get a single component
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/components/1

# Create a component (ENGINEER/ADMIN only)
curl -X POST http://localhost:8080/api/components \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "payment-service",
    "type": "SERVICE",
    "owner": "payments-team@example.com",
    "teamId": 1,
    "status": "ACTIVE",
    "description": "Handles payment processing and reconciliation",
    "repositoryUrl": "https://github.com/edap/payment-service",
    "metadata": {
      "language": "Java",
      "runtime": "JVM17",
      "sla": "99.99%"
    }
  }'

# Update a component
curl -X PUT http://localhost:8080/api/components/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"api-gateway","type":"GATEWAY","status":"DEPRECATED","metadata":{}}'

# Delete a component (ADMIN only)
curl -X DELETE http://localhost:8080/api/components/1 \
  -H "Authorization: Bearer $TOKEN"
```

### Teams

```bash
# List teams
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/teams

# Create a team
curl -X POST http://localhost:8080/api/teams \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "mobile",
    "description": "Mobile engineering team",
    "slackChannel": "#mobile-eng",
    "email": "mobile@example.com"
  }'
```

### Dependency Graph (Neo4j)

```bash
# Create a dependency edge
curl -X POST http://localhost:8080/api/dependencies \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "sourceComponentId": 1,
    "targetComponentId": 2,
    "type": "DEPENDS_ON",
    "weight": 0.9,
    "description": "API Gateway routes traffic to User Service"
  }'

# Get dependency graph (3-hop BFS)
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/dependencies/graph/1?depth=3"

# Get direct outgoing dependencies
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/dependencies/1/outgoing

# Get incoming dependents (who depends on this component)
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/dependencies/2/incoming
```

---

## gRPC Examples

Using [grpcurl](https://github.com/fullstorydev/grpcurl):

```bash
# List available services
grpcurl -plaintext localhost:9090 list

# Get a single component
grpcurl -plaintext \
  -d '{"id": "1"}' \
  localhost:9090 com.edap.grpc.ComponentService/GetComponent

# List components with filters
grpcurl -plaintext \
  -d '{"team": "platform", "status": "ACTIVE", "page": 0, "page_size": 10}' \
  localhost:9090 com.edap.grpc.ComponentService/ListComponents

# Get dependency graph
grpcurl -plaintext \
  -d '{"root_component_id": "1", "depth": 3}' \
  localhost:9090 com.edap.grpc.ComponentService/GetDependencyGraph
```

---

## Metrics

Custom metrics exposed at `/actuator/prometheus`:

| Metric | Type | Description |
|--------|------|-------------|
| `edap_component_lookup_total` | Counter | Total component lookup requests (REST + gRPC) |
| `edap_dependency_graph_nodes` | Gauge | Current count of nodes in Neo4j graph |

Standard Spring Boot metrics also available: JVM, HTTP request duration, HikariCP pool stats, etc.

---

## Running Tests

```bash
# Unit tests only (fast, no Docker)
mvn test -Dtest="ComponentServiceTest,ComponentControllerTest"

# Integration tests (use H2 in-memory, no external services)
mvn test -Dtest="AuthControllerIntegrationTest"

# All tests
mvn test

# With coverage report
mvn test jacoco:report
```

---

## Kubernetes Deployment

```bash
# Create namespace
kubectl apply -f k8s/namespace.yaml

# Create secrets (update values first!)
kubectl -n edap create secret generic edap-secrets \
  --from-literal=SPRING_DATASOURCE_PASSWORD=edap_pass \
  --from-literal=SPRING_NEO4J_AUTHENTICATION_PASSWORD=neo4jpass \
  --from-literal=EDAP_SECURITY_JWT_SECRET=<your-256bit-base64-secret>

# Deploy MySQL
kubectl apply -f k8s/mysql-statefulset.yaml

# Deploy Neo4j
kubectl apply -f k8s/neo4j-statefulset.yaml

# Apply configmap
kubectl apply -f k8s/configmap.yaml

# Deploy the application
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml

# Check rollout
kubectl -n edap rollout status deployment/edap-app

# Port-forward for local access
kubectl -n edap port-forward svc/edap-service 8080:80 9090:9090
```

---

## Project Structure

```
edap/
├── src/
│   ├── main/
│   │   ├── java/com/edap/
│   │   │   ├── config/         # SecurityConfig, Neo4jConfig, AppConfig (metrics)
│   │   │   ├── controller/     # REST: Component, Dependency, Team, Auth
│   │   │   ├── dto/            # Request/Response DTOs, ApiResponse wrapper
│   │   │   ├── entity/         # JPA: Component, Team, AppUser | Neo4j: ComponentNode, DependencyRelationship
│   │   │   ├── exception/      # GlobalExceptionHandler + custom exceptions
│   │   │   ├── grpc/           # EntityGrpcService (generated stub impl)
│   │   │   ├── repository/     # JPA + Neo4j repositories
│   │   │   ├── security/       # JwtTokenProvider, JwtAuthenticationFilter, UserPrincipal
│   │   │   └── service/        # ComponentService, DependencyService, TeamService, AuthService
│   │   ├── proto/
│   │   │   └── entity_service.proto
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-docker.yml
│   │       ├── prometheus.yml
│   │       └── db/migration/
│   │           └── V1__init_schema.sql
│   └── test/
│       ├── java/com/edap/
│       │   ├── service/        # ComponentServiceTest (Mockito unit tests)
│       │   ├── controller/     # ComponentControllerTest (standalone MockMvc)
│       │   └── integration/    # AuthControllerIntegrationTest (full context + H2)
│       └── resources/
│           └── application-test.yml
├── k8s/
│   ├── namespace.yaml
│   ├── configmap.yaml
│   ├── deployment.yaml
│   ├── service.yaml
│   ├── mysql-statefulset.yaml
│   └── neo4j-statefulset.yaml
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```
