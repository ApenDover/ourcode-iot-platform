package ts.andrey.orchestrator.application.service;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ts.andrey.orchestrator.application.port.DeviceServicePort;
import ts.andrey.orchestrator.application.port.RouterManagerPort;
import ts.andrey.orchestrator.domain.exception.RouterManagerRollbackException;
import ts.andrey.orchestrator.dto.ApiV1DevicesDeviceIdVersionPost200Response;
import ts.andrey.orchestrator.dto.ApiV1DevicesDeviceIdVersionPostRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateDeviceVersionUseCase {

    private final RouterManagerPort routerManagerPort;
    private final DeviceServicePort deviceServicePort;

    private static final String COMMAND = "UPDATE_VERSION";

    @WithSpan("SagaUpdateDeviceVersion")
    public ApiV1DevicesDeviceIdVersionPost200Response updateDeviceVersion(
            String deviceId,
            ApiV1DevicesDeviceIdVersionPostRequest apiV1DevicesDeviceIdVersionPostRequest
    ) {
        final var device = deviceServicePort.getDevice(deviceId);
        final var deviceVersionResponse = deviceServicePort.updateDeviceVersion(
                deviceId,
                apiV1DevicesDeviceIdVersionPostRequest.getTargetVersion(),
                device.getEtag()
        );

        Integer commandId;
        try {
            commandId = routerManagerPort.sendCommand(
                    deviceId,
                    COMMAND,
                    apiV1DevicesDeviceIdVersionPostRequest.getTargetVersion()
            );
        } catch (Exception ex) {
            deviceServicePort.rollbackDeviceVersion(deviceId, device.getVersion(), device.getEtag() + 1);
            log.error("UpdateDeviceVersion fail: {}", ex.getMessage(), ex);
            throw new RouterManagerRollbackException();
        }

        final var result = new ApiV1DevicesDeviceIdVersionPost200Response();
        result.setDeviceId(deviceId);
        result.setPrevVersion(deviceVersionResponse.getPrevVersion());
        result.setTargetVersion(deviceVersionResponse.getTargetVersion());
        result.setCommandId(String.valueOf(commandId));
        return result;
    }

}
