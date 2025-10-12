package ts.andrey.orchestrator.infrastructure.adapter.out;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ts.andrey.orchestrator.dto.Device;
import ts.andrey.orchestrator.dto.DeviceCreateRequest;
import ts.andrey.orchestrator.dto.DeviceUpdateRequest;
import ts.andrey.orchestrator.infrastructure.feign.DeviceServiceClient;
import ts.andrey.orchestrator.infrastructure.mapper.DeviceMapper;
import ts.andrey.orchestrator.application.outport.DeviceServicePort;

@Component
@RequiredArgsConstructor
public class DeviceServiceAdapter implements DeviceServicePort {

    private final DeviceServiceClient deviceServiceClient;
    private final DeviceMapper deviceMapper;

    @Override
    public Device createDevice(DeviceCreateRequest deviceCreateRequest) {
        final var request = deviceMapper.toDeviceCreateRequestDto(deviceCreateRequest);
        final var response = deviceServiceClient.createDevice(request);
        return deviceMapper.toOrchestratorDeviceDto(response.getBody());
    }

    @Override
    public Device updateDevice(String deviceId, DeviceUpdateRequest deviceUpdateRequest) {
        final var request = deviceMapper.toDeviceUpdateRequestDto(deviceUpdateRequest);
        final var response = deviceServiceClient.updateDevice(deviceId, request);
        return deviceMapper.toOrchestratorDeviceDto(response.getBody());
    }

    @Override
    public void deleteDevice(String deviceId) {
        deviceServiceClient.deleteDevice(deviceId);
    }

    @Override
    public Device getDevice(String deviceId) {
        final var response = deviceServiceClient.getDevice(deviceId);
        return deviceMapper.toOrchestratorDeviceDto(response.getBody());
    }

}
