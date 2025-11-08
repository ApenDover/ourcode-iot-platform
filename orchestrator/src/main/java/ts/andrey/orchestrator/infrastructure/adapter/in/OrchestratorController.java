package ts.andrey.orchestrator.infrastructure.adapter.in;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ts.andrey.orchestrator.api.DefaultApi;
import ts.andrey.orchestrator.application.outport.DeviceServicePort;
import ts.andrey.orchestrator.application.outport.EventServicePort;
import ts.andrey.orchestrator.application.outport.RouterManagerGrpcPort;
import ts.andrey.orchestrator.application.service.UpdateDeviceVersionUseCase;
import ts.andrey.orchestrator.dto.AckCommandRequest;
import ts.andrey.orchestrator.dto.AckCommandResponse;
import ts.andrey.orchestrator.dto.ApiV1DevicesDeviceIdVersionPost200Response;
import ts.andrey.orchestrator.dto.ApiV1DevicesDeviceIdVersionPostRequest;
import ts.andrey.orchestrator.dto.Device;
import ts.andrey.orchestrator.dto.DeviceCreateRequest;
import ts.andrey.orchestrator.dto.DeviceUpdateRequest;
import ts.andrey.orchestrator.dto.Event;
import ts.andrey.orchestrator.dto.EventPage;
import ts.andrey.orchestrator.dto.PollCommandsResponse;
import ts.andrey.orchestrator.dto.SendCommandRequest;
import ts.andrey.orchestrator.dto.SendCommandResponse;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class OrchestratorController implements DefaultApi {

    private final RouterManagerGrpcPort routerManagerGrpcPort;
    private final DeviceServicePort deviceServicePort;
    private final EventServicePort eventServicePort;
    private final UpdateDeviceVersionUseCase updateDeviceVersionUseCase;

    @Override
    public ResponseEntity<AckCommandResponse> apiV1CommandsAckPost(AckCommandRequest ackCommandRequest) {
        final var response = routerManagerGrpcPort.ackCommand(ackCommandRequest);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<PollCommandsResponse> apiV1CommandsPollGet(String routerSerial) {
        final var response = routerManagerGrpcPort.pollCommands(routerSerial);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<SendCommandResponse> apiV1CommandsPost(SendCommandRequest sendCommandRequest) {
        final var response = routerManagerGrpcPort.sendCommand(sendCommandRequest);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Void> apiV1DevicesDeviceIdDelete(String deviceId) {
        deviceServicePort.deleteDevice(deviceId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Device> apiV1DevicesDeviceIdGet(String deviceId) {
        final var response = deviceServicePort.getDevice(deviceId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Device> apiV1DevicesDeviceIdPut(String deviceId, DeviceUpdateRequest deviceUpdateRequest) {
        final var response = deviceServicePort.updateDevice(deviceId, deviceUpdateRequest);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ApiV1DevicesDeviceIdVersionPost200Response> apiV1DevicesDeviceIdVersionPost(
            String deviceId, ApiV1DevicesDeviceIdVersionPostRequest apiV1DevicesDeviceIdVersionPostRequest
    ) {
        final var response = updateDeviceVersionUseCase.updateDeviceVersion(
                deviceId, apiV1DevicesDeviceIdVersionPostRequest
        );
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<List<Device>> apiV1DevicesGet() {
        final var device = deviceServicePort.getDevice("");
        return ResponseEntity.ok(List.of(device));
    }

    @Override
    public ResponseEntity<Device> apiV1DevicesPost(DeviceCreateRequest deviceCreateRequest) {
        final var device = deviceServicePort.createDevice(deviceCreateRequest);
        return ResponseEntity.ok(device);
    }

    @Override
    public ResponseEntity<Event> apiV1EventsEventIdGet(String eventId, String deviceId) {
        final var event = eventServicePort.getEvent(eventId, deviceId);
        return ResponseEntity.ok(event);
    }

    @Override
    public ResponseEntity<EventPage> apiV1EventsGet(
            String deviceId, Long fromTimestamp, Long toTimestamp,
            String type, Integer page, Integer size
    ) {
        final var response = eventServicePort.getEventByFilter(deviceId, fromTimestamp, toTimestamp, type, page, size);
        return ResponseEntity.ok(response);
    }

}
