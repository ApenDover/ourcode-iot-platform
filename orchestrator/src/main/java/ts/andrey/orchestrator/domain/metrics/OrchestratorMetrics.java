package ts.andrey.orchestrator.domain.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrchestratorMetrics {

    private final MeterRegistry meterRegistry;

    public void routerAckSuccess() {
        meterRegistry.counter("orchestrator_router_ack_success").increment();
    }

    public void routerAckFail() {
        meterRegistry.counter("orchestrator_router_ack_fail").increment();
    }

    public void routerPollSuccess() {
        meterRegistry.counter("orchestrator_router_poll_success").increment();
    }

    public void routerPollFail() {
        meterRegistry.counter("orchestrator_router_poll_fail").increment();
    }

    public void routerCommandSuccess() {
        meterRegistry.counter("orchestrator_router_command_success").increment();
    }

    public void routerCommandFail() {
        meterRegistry.counter("orchestrator_router_command_fail").increment();
    }

    public void deviceServiceGetSuccess() {
        meterRegistry.counter("orchestrator_deviceService_get_success").increment();
    }

    public void deviceServiceGetFail() {
        meterRegistry.counter("orchestrator_deviceService_get_fail").increment();
    }

    public void deviceServiceGetListSuccess() {
        meterRegistry.counter("orchestrator_deviceService_getList_success").increment();
    }

    public void deviceServiceGetListFail() {
        meterRegistry.counter("orchestrator_deviceService_getList_fail").increment();
    }

    public void eventServiceGetSuccess() {
        meterRegistry.counter("orchestrator_eventService_get_success").increment();
    }

    public void eventServiceGetFail() {
        meterRegistry.counter("orchestrator_eventService_get_fail").increment();
    }

    public void eventServiceGetFilterSuccess() {
        meterRegistry.counter("orchestrator_eventService_getFilter_success").increment();
    }

    public void eventServiceGetFilterFail() {
        meterRegistry.counter("orchestrator_eventService_getFilter_fail").increment();
    }

    public void deviceServiceDeleteSuccess() {
        meterRegistry.counter("orchestrator_deviceService_delete_success").increment();
    }

    public void deviceServiceDeleteFail() {
        meterRegistry.counter("orchestrator_deviceService_delete_fail").increment();
    }

    public void deviceUpdateSuccess() {
        meterRegistry.counter("orchestrator_deviceService_update_success").increment();
    }

    public void deviceUpdateFail() {
        meterRegistry.counter("orchestrator_deviceService_update_fail").increment();
    }

    public void sagaUpdateVersionSuccess() {
        meterRegistry.counter("orchestrator_saga_success").increment();
    }

    public void sagaUpdateVersionRollback() {
        meterRegistry.counter("orchestrator_saga_rollback").increment();
    }

    public void sagaUpdateVersionFail() {
        meterRegistry.counter("orchestrator_saga_fail").increment();
    }

    public void orchestratorRedisSuccess() {
        meterRegistry.counter("orchestrator_redis_success").increment();
    }

    public void orchestratorRedisFailure() {
        meterRegistry.counter("orchestrator_redis_fail").increment();
    }

    public void recordFailure(String method, String uri, int status, Throwable ex) {
        meterRegistry.counter("orchestrator_fail",
                "method", method,
                "uri", normalizeUri(uri),
                "status", String.valueOf(status),
                "statusGroup", statusGroup(status),
                "exception", ex != null ? ex.getClass().getSimpleName() : "unknown"
        ).increment();
    }

    private String statusGroup(int status) {
        final var httpStatus = HttpStatusCode.valueOf(status);
        if (httpStatus.is2xxSuccessful()) {
            return "2xx";
        }
        if (httpStatus.is4xxClientError()) {
            return "4xx";
        }
        if (httpStatus.is5xxServerError()) {
            return "5xx";
        }
        return "other";
    }

    private String normalizeUri(String uri) {
        return uri.replaceAll("/[0-9A-Z]{26}$", "/{id}");
    }

}
