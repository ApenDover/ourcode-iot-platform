package ts.andrey.orchestrator.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ts.andrey.orchestrator.application.outport.DeviceServicePort;
import ts.andrey.orchestrator.domain.exception.RouterManagerRollbackException;
import ts.andrey.orchestrator.dto.ApiV1DevicesDeviceIdVersionPost200Response;
import ts.andrey.orchestrator.dto.ApiV1DevicesDeviceIdVersionPost502Response;
import ts.andrey.orchestrator.dto.ApiV1DevicesDeviceIdVersionPostRequest;
import ts.andrey.orchestrator.infrastructure.adapter.out.RouterManagerAdapter;

@Service
@RequiredArgsConstructor
public class UpdateDeviceVersionUseCase {

    private final RouterManagerAdapter routerManagerAdapter;
    private final DeviceServicePort deviceServicePort;

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
            commandId = routerManagerAdapter.sendCommand(
                    deviceId,
                    "UPDATE VERSION",
                    apiV1DevicesDeviceIdVersionPostRequest.getTargetVersion()
            );
        } catch (Exception ex) {
            deviceServicePort.rollbackDeviceVersion(deviceId, device.getVersion(), device.getEtag() + 1);
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
