package ts.andrey.orchestrator.infrastructure.adapter.out;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ts.andrey.device.model.DeviceStatus;
import ts.andrey.device.model.DeviceVersionResponse;
import ts.andrey.device.model.DeviceVersionRollbackRequest;
import ts.andrey.device.model.DeviceVersionUpdateRequest;
import ts.andrey.orchestrator.application.port.DeviceServicePort;
import ts.andrey.orchestrator.domain.exception.DeviceServiceRollbackException;
import ts.andrey.orchestrator.dto.Device;
import ts.andrey.orchestrator.dto.DeviceCreateRequest;
import ts.andrey.orchestrator.dto.DeviceUpdateRequest;
import ts.andrey.orchestrator.infrastructure.out.port.feign.DeviceServiceClient;
import ts.andrey.orchestrator.infrastructure.mapper.DeviceMapper;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DeviceServiceAdapter implements DeviceServicePort {

    private final DeviceServiceClient deviceServiceClient;
    private final DeviceMapper deviceMapper;

    @Override
    @WithSpan("DeviceTransportCreate")
    public Device createDevice(DeviceCreateRequest deviceCreateRequest) {
        final var request = deviceMapper.toDeviceCreateRequestDto(deviceCreateRequest);
        final var response = deviceServiceClient.createDevice(request);
        return deviceMapper.toOrchestratorDeviceDto(response.getBody());
    }

    @Override
    @WithSpan("DeviceTransportUpdate")
    public Device updateDevice(String deviceId, DeviceUpdateRequest deviceUpdateRequest) {
        final var request = deviceMapper.toDeviceUpdateRequestDto(deviceUpdateRequest);
        final var response = deviceServiceClient.updateDevice(deviceId, request);
        return deviceMapper.toOrchestratorDeviceDto(response.getBody());
    }

    @Override
    @WithSpan("DeviceTransportDeleteById")
    public void deleteDevice(String deviceId) {
        deviceServiceClient.deleteDevice(deviceId);
    }

    @Override
    @WithSpan("DeviceTransportGetById")
    public Device getDevice(String deviceId) {
        final var response = deviceServiceClient.getDevice(deviceId);
        return deviceMapper.toOrchestratorDeviceDto(response.getBody());
    }

    @Override
    @WithSpan("DeviceTransportGetAll")
    public List<Device> getDevices() {
        final var response = deviceServiceClient.getDevices();
        return deviceMapper.toOrchestratorDeviceDtos(response.getBody());
    }

    @Override
    @WithSpan("DeviceTransportUpdateVersion")
    public DeviceVersionResponse updateDeviceVersion(String deviceId, String deviceVersion, Long etag) {
        final var request = new DeviceVersionUpdateRequest();
        request.setEtag(etag);
        request.setTargetVersion(deviceVersion);
        final var deviceVersionResponse = deviceServiceClient.updateDeviceVersion(deviceId, request);
        return deviceVersionResponse.getBody();
    }

    @Override
    @WithSpan("DeviceTransportRollbackVersion")
    public DeviceVersionResponse rollbackDeviceVersion(String deviceId, String deviceVersion, Long etag) {
        try {
            final var request = new DeviceVersionRollbackRequest();
            request.setEtag(etag);
            request.setRollbackVersion(deviceVersion);
            request.setStatus(DeviceStatus.READY);
            final var deviceVersionResponse = deviceServiceClient.rollbackDeviceVersion(deviceId, request);
            return deviceVersionResponse.getBody();
        } catch (Exception ex) {
            throw new DeviceServiceRollbackException(ex);
        }
    }

}
