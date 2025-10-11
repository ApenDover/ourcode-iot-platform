package ts.andrey.orchestrator.in;

import org.springframework.http.ResponseEntity;
import ts.andrey.orchestrator.api.DefaultApi;
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

public class OrchestratorController implements DefaultApi {

    @Override
    public ResponseEntity<AckCommandResponse> apiV1CommandsAckPost(AckCommandRequest ackCommandRequest) {
        return DefaultApi.super.apiV1CommandsAckPost(ackCommandRequest);
    }

    @Override
    public ResponseEntity<PollCommandsResponse> apiV1CommandsPollGet(String routerSerial) {
        return DefaultApi.super.apiV1CommandsPollGet(routerSerial);
    }

    @Override
    public ResponseEntity<SendCommandResponse> apiV1CommandsPost(SendCommandRequest sendCommandRequest) {
        return DefaultApi.super.apiV1CommandsPost(sendCommandRequest);
    }

    @Override
    public ResponseEntity<Void> apiV1DevicesDeviceIdDelete(String deviceId) {
        return DefaultApi.super.apiV1DevicesDeviceIdDelete(deviceId);
    }

    @Override
    public ResponseEntity<Device> apiV1DevicesDeviceIdGet(String deviceId) {
        return DefaultApi.super.apiV1DevicesDeviceIdGet(deviceId);
    }

    @Override
    public ResponseEntity<Device> apiV1DevicesDeviceIdPut(String deviceId, DeviceUpdateRequest deviceUpdateRequest) {
        return DefaultApi.super.apiV1DevicesDeviceIdPut(deviceId, deviceUpdateRequest);
    }

    @Override
    public ResponseEntity<ApiV1DevicesDeviceIdVersionPost200Response> apiV1DevicesDeviceIdVersionPost(String deviceId, ApiV1DevicesDeviceIdVersionPostRequest apiV1DevicesDeviceIdVersionPostRequest) {
        return DefaultApi.super.apiV1DevicesDeviceIdVersionPost(deviceId, apiV1DevicesDeviceIdVersionPostRequest);
    }

    @Override
    public ResponseEntity<List<Device>> apiV1DevicesGet() {
        return DefaultApi.super.apiV1DevicesGet();
    }

    @Override
    public ResponseEntity<Device> apiV1DevicesPost(DeviceCreateRequest deviceCreateRequest) {
        return DefaultApi.super.apiV1DevicesPost(deviceCreateRequest);
    }

    @Override
    public ResponseEntity<Event> apiV1EventsEventIdGet(String eventId, String deviceId) {
        return DefaultApi.super.apiV1EventsEventIdGet(eventId, deviceId);
    }

    @Override
    public ResponseEntity<EventPage> apiV1EventsGet(String deviceId, Long fromTimestamp, Long toTimestamp, String type, Integer page, Integer size) {
        return DefaultApi.super.apiV1EventsGet(deviceId, fromTimestamp, toTimestamp, type, page, size);
    }

}
