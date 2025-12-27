package ts.andrey.orchestrator.infrastructure.adapter.in;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ts.andrey.orchestrator.api.DefaultApi;
import ts.andrey.orchestrator.application.port.DeviceServicePort;
import ts.andrey.orchestrator.application.port.EventServicePort;
import ts.andrey.orchestrator.application.port.RouterManagerGrpcPort;
import ts.andrey.orchestrator.application.service.IdempotentProcessor;
import ts.andrey.orchestrator.application.service.UpdateDeviceVersionUseCase;
import ts.andrey.orchestrator.domain.metrics.OrchestratorMetrics;
import ts.andrey.orchestrator.dto.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class OrchestratorController implements DefaultApi {

    private final RouterManagerGrpcPort routerManagerGrpcPort;
    private final DeviceServicePort deviceServicePort;
    private final EventServicePort eventServicePort;
    private final UpdateDeviceVersionUseCase updateDeviceVersionUseCase;
    private final IdempotentProcessor idempotentProcessor;
    private final OrchestratorMetrics orchestratorMetrics;

    @Override
    @WithSpan("ControllerRouterManagerAck")
    public ResponseEntity<AckCommandResponse> apiV1CommandsAckPost(AckCommandRequest ackCommandRequest) {
        final var response = routerManagerGrpcPort.ackCommand(ackCommandRequest);
        orchestratorMetrics.routerAckSuccess();
        return ResponseEntity.ok(response);
    }

    @Override
    @WithSpan("ControllerRouterManagerPoll")
    public ResponseEntity<PollCommandsResponse> apiV1CommandsPollGet(String routerSerial) {
        final var response = routerManagerGrpcPort.pollCommands(routerSerial);
        orchestratorMetrics.routerPollSuccess();
        return ResponseEntity.ok(response);
    }

    @Override
    @WithSpan("ControllerRouterManagerCommand")
    public ResponseEntity<SendCommandResponse> apiV1CommandsPost(SendCommandRequest sendCommandRequest) {
        final var response = routerManagerGrpcPort.sendCommand(sendCommandRequest);
        orchestratorMetrics.routerCommandSuccess();
        return ResponseEntity.ok(response);
    }

    @Override
    @WithSpan("ControllerDeleteDevice")
    public ResponseEntity<Void> apiV1DevicesDeviceIdDelete(String deviceId) {
        deviceServicePort.deleteDevice(deviceId);
        orchestratorMetrics.deviceServiceDeleteSuccess();
        return ResponseEntity.ok().build();
    }

    @Override
    @WithSpan("ControllerGetDevice")
    public ResponseEntity<Device> apiV1DevicesDeviceIdGet(String deviceId) {
        final var response = deviceServicePort.getDevice(deviceId);
        orchestratorMetrics.deviceServiceGetSuccess();
        return ResponseEntity.ok(response);
    }

    @Override
    @WithSpan("ControllerUpdateDevice")
    public ResponseEntity<Device> apiV1DevicesDeviceIdPut(String deviceId, DeviceUpdateRequest deviceUpdateRequest) {
        final var response = deviceServicePort.updateDevice(deviceId, deviceUpdateRequest);
        orchestratorMetrics.deviceUpdateSuccess();
        return ResponseEntity.ok(response);
    }

    @Override
    @WithSpan("ControllerUpdateVersionSaga")
    public ResponseEntity<ApiV1DevicesDeviceIdVersionPost200Response> apiV1DevicesDeviceIdVersionPost(
            String deviceId, ApiV1DevicesDeviceIdVersionPostRequest apiV1DevicesDeviceIdVersionPostRequest
    ) {
        final var cashed = idempotentProcessor.checkIdempotentKey(
                apiV1DevicesDeviceIdVersionPostRequest.getIdempotencyKey()
        );
        if (cashed.isPresent()) {
            return ResponseEntity.ok(cashed.get());
        }
        final var response = updateDeviceVersionUseCase.updateDeviceVersion(
                deviceId, apiV1DevicesDeviceIdVersionPostRequest
        );
        idempotentProcessor.saveResponse(apiV1DevicesDeviceIdVersionPostRequest.getIdempotencyKey(), response);
        orchestratorMetrics.sagaUpdateVersionSuccess();
        return ResponseEntity.ok(response);
    }

    @Override
    @WithSpan("ControllerGetAllDevices")
    public ResponseEntity<List<Device>> apiV1DevicesGet() {
        final var device = deviceServicePort.getDevices();
        orchestratorMetrics.deviceServiceGetListSuccess();
        return ResponseEntity.ok(device);
    }

    @Override
    @WithSpan("ControllerCreateDevice")
    public ResponseEntity<Device> apiV1DevicesPost(DeviceCreateRequest deviceCreateRequest) {
        final var device = deviceServicePort.createDevice(deviceCreateRequest);
        orchestratorMetrics.deviceUpdateSuccess();
        return ResponseEntity.ok(device);
    }

    @Override
    @WithSpan("ControllerGetEvent")
    public ResponseEntity<Event> apiV1EventsEventIdGet(String eventId, String deviceId) {
        final var event = eventServicePort.getEvent(eventId, deviceId);
        orchestratorMetrics.eventServiceGetSuccess();
        return ResponseEntity.ok(event);
    }

    @Override
    @WithSpan("ControllerGetEventsByFilter")
    public ResponseEntity<EventPage> apiV1EventsGet(
            String deviceId, Long fromTimestamp, Long toTimestamp,
            String type, Integer page, Integer size
    ) {
        final var response = eventServicePort.getEventByFilter(deviceId, fromTimestamp, toTimestamp, type, page, size);
        orchestratorMetrics.eventServiceGetFilterSuccess();
        return ResponseEntity.ok(response);
    }

}
