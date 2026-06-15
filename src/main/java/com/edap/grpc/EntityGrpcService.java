package com.edap.grpc;

import com.edap.dto.ComponentResponse;
import com.edap.dto.DependencyGraphResponse;
import com.edap.dto.DependencyResponse;
import com.edap.exception.ResourceNotFoundException;
import com.edap.service.ComponentService;
import com.edap.service.DependencyService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Map;

/**
 * gRPC service implementation for ComponentService.
 * Wraps the same service layer used by the REST controllers.
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class EntityGrpcService extends ComponentServiceGrpc.ComponentServiceImplBase {

    private final ComponentService componentService;
    private final DependencyService dependencyService;

    @Override
    public void getComponent(GetComponentRequest request,
                             StreamObserver<GetComponentResponse> responseObserver) {
        log.info("[gRPC] GetComponent id={}", request.getId());
        try {
            long id = Long.parseLong(request.getId());
            ComponentResponse comp = componentService.getById(id);
            GetComponentResponse response = GetComponentResponse.newBuilder()
                    .setComponent(toProto(comp))
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (ResourceNotFoundException ex) {
            log.warn("[gRPC] Component not found: {}", ex.getMessage());
            responseObserver.onError(
                    Status.NOT_FOUND.withDescription(ex.getMessage()).asRuntimeException());
        } catch (NumberFormatException ex) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT.withDescription("id must be a numeric value").asRuntimeException());
        } catch (Exception ex) {
            log.error("[gRPC] GetComponent error", ex);
            responseObserver.onError(
                    Status.INTERNAL.withDescription("Internal server error").asRuntimeException());
        }
    }

    @Override
    public void listComponents(ListComponentsRequest request,
                               StreamObserver<ListComponentsResponse> responseObserver) {
        log.info("[gRPC] ListComponents team={} status={} page={} pageSize={}",
                request.getTeam(), request.getStatus(), request.getPage(), request.getPageSize());
        try {
            int page = Math.max(0, request.getPage());
            int size = request.getPageSize() > 0 ? request.getPageSize() : 20;

            String team   = request.getTeam().isBlank()   ? null : request.getTeam();
            String status = request.getStatus().isBlank() ? null : request.getStatus();

            Page<ComponentResponse> result = componentService.list(
                    team, status, null, PageRequest.of(page, size, Sort.by("name")));

            ListComponentsResponse.Builder builder = ListComponentsResponse.newBuilder()
                    .setTotalCount(result.getTotalElements());
            result.forEach(c -> builder.addComponents(toProto(c)));

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception ex) {
            log.error("[gRPC] ListComponents error", ex);
            responseObserver.onError(
                    Status.INTERNAL.withDescription(ex.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getDependencyGraph(GetDependencyGraphRequest request,
                                   StreamObserver<GetDependencyGraphResponse> responseObserver) {
        log.info("[gRPC] GetDependencyGraph rootId={} depth={}",
                request.getRootComponentId(), request.getDepth());
        try {
            long rootId = Long.parseLong(request.getRootComponentId());
            int depth = request.getDepth() > 0 ? request.getDepth() : 3;

            DependencyGraphResponse graph = dependencyService.getDependencyGraph(rootId, depth);

            GetDependencyGraphResponse.Builder builder = GetDependencyGraphResponse.newBuilder();
            graph.getNodes().forEach(n -> builder.addNodes(toProto(n)));
            for (DependencyResponse edge : graph.getEdges()) {
                builder.addEdges(DependencyEdge.newBuilder()
                        .setSourceComponentId(String.valueOf(edge.getSourceComponentId()))
                        .setTargetComponentId(String.valueOf(edge.getTargetComponentId()))
                        .setType(edge.getType() != null ? edge.getType() : "")
                        .setWeight(edge.getWeight() != null ? edge.getWeight() : 1.0)
                        .build());
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (ResourceNotFoundException ex) {
            responseObserver.onError(
                    Status.NOT_FOUND.withDescription(ex.getMessage()).asRuntimeException());
        } catch (NumberFormatException ex) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("rootComponentId must be numeric").asRuntimeException());
        } catch (Exception ex) {
            log.error("[gRPC] GetDependencyGraph error", ex);
            responseObserver.onError(
                    Status.INTERNAL.withDescription("Internal server error").asRuntimeException());
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ComponentProto toProto(ComponentResponse c) {
        ComponentProto.Builder b = ComponentProto.newBuilder()
                .setId(String.valueOf(c.getId()))
                .setName(nvl(c.getName()))
                .setType(nvl(c.getType()))
                .setOwner(nvl(c.getOwner()))
                .setTeam(nvl(c.getTeamName()))
                .setStatus(nvl(c.getStatus()))
                .setCreatedAt(c.getCreatedAt() != null ? c.getCreatedAt().toString() : "")
                .setUpdatedAt(c.getUpdatedAt() != null ? c.getUpdatedAt().toString() : "");

        if (c.getMetadata() != null) {
            for (Map.Entry<String, String> entry : c.getMetadata().entrySet()) {
                b.putMetadata(entry.getKey(), entry.getValue());
            }
        }
        return b.build();
    }

    private String nvl(String s) {
        return s != null ? s : "";
    }
}
