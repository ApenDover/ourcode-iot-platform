package ts.andrey.orchestrator.out.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ts.andrey.orchestrator.dto.Device;
import ts.andrey.orchestrator.dto.DeviceCreateRequest;
import ts.andrey.orchestrator.dto.DeviceUpdateRequest;
import ts.andrey.orchestrator.mapper.DeviceMapper;
import ts.andrey.orchestrator.out.port.DeviceServicePort;

@Component
@RequiredArgsConstructor
public class DeviceServiceAdapter {

    private final DeviceServicePort deviceServicePort;
    private final DeviceMapper deviceMapper;

    public Device createDevice(DeviceCreateRequest deviceCreateRequest) {
        final var request = deviceMapper.toDeviceCreateRequestDto(deviceCreateRequest);
        final var response = deviceServicePort.createDevice(request);
        return deviceMapper.toOrchestratorDeviceDto(response.getBody());
    }

    public Device updateDevice(String deviceId, DeviceUpdateRequest deviceUpdateRequest) {
        final var request = deviceMapper.toDeviceUpdateRequestDto(deviceUpdateRequest);
        final var response = deviceServicePort.updateDevice(deviceId, request);
        return deviceMapper.toOrchestratorDeviceDto(response.getBody());
    }

}
