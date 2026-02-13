package ts.andrey.orchestrator.infrastructure.adapter.out;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import ts.andrey.orchestrator.infrastructure.mapper.DeviceMapper;
import ts.andrey.orchestrator.infrastructure.out.port.feign.DeviceServiceClient;
import ts.andrey.orchestrator.infrastructure.util.RequestTimerUtil;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceServiceAdapter implements DeviceServicePort {

    private final DeviceServiceClient deviceServiceClient;
    private final DeviceMapper deviceMapper;
    private final RequestTimerUtil requestTimerUtil;

    @Override
    @WithSpan("DeviceTransportCreate")
    public Device createDevice(DeviceCreateRequest deviceCreateRequest) {
        return requestTimerUtil.recordExternal("device-service", "createDevice", "http", () -> {
            final var request = deviceMapper.toDeviceCreateRequestDto(deviceCreateRequest);
            final var response = deviceServiceClient.createDevice(request);
            return deviceMapper.toOrchestratorDeviceDto(response.getBody());
        });
    }

    @Override
    @WithSpan("DeviceTransportUpdate")
    public Device updateDevice(String deviceId, DeviceUpdateRequest deviceUpdateRequest) {
        return requestTimerUtil.recordExternal("device-service", "updateDevice", "http", () -> {
            final var request = deviceMapper.toDeviceUpdateRequestDto(deviceUpdateRequest);
            final var response = deviceServiceClient.updateDevice(deviceId, request);
            return deviceMapper.toOrchestratorDeviceDto(response.getBody());
        });
    }

    @Override
    @WithSpan("DeviceTransportDeleteById")
    public void deleteDevice(String deviceId) {
        requestTimerUtil.recordExternal("device-service", "deleteDevice", "http", () -> deviceServiceClient.deleteDevice(deviceId));
    }

    @Override
    @WithSpan("DeviceTransportGetById")
    public Device getDevice(String deviceId) {
        return requestTimerUtil.recordExternal("device-service", "getDevice", "http", () -> {
            final var response = deviceServiceClient.getDevice(deviceId);
            return deviceMapper.toOrchestratorDeviceDto(response.getBody());
        });
    }

    @Override
    @WithSpan("DeviceTransportGetAll")
    public List<Device> getDevices() {
        return requestTimerUtil.recordExternal("device-service", "getDevices", "http", () -> {
            final var response = deviceServiceClient.getDevices();
            return deviceMapper.toOrchestratorDeviceDtos(response.getBody());
        });
    }

    @Override
    @WithSpan("DeviceTransportUpdateVersion")
    public DeviceVersionResponse updateDeviceVersion(String deviceId, String deviceVersion, Long etag) {
        return requestTimerUtil.recordExternal("device-service", "updateDeviceVersion", "http", () -> {
            final var request = new DeviceVersionUpdateRequest();
            request.setEtag(etag);
            request.setTargetVersion(deviceVersion);
            final var deviceVersionResponse = deviceServiceClient.updateDeviceVersion(deviceId, request);
            return deviceVersionResponse.getBody();
        });
    }

    @Override
    @WithSpan("DeviceTransportRollbackVersion")
    public DeviceVersionResponse rollbackDeviceVersion(String deviceId, String deviceVersion, Long etag) {
        try {
            return requestTimerUtil.recordExternal("device-service", "rollbackDeviceVersion", "http", () -> {
                final var request = new DeviceVersionRollbackRequest();
                request.setEtag(etag);
                request.setRollbackVersion(deviceVersion);
                request.setStatus(DeviceStatus.READY);
                final var deviceVersionResponse = deviceServiceClient.rollbackDeviceVersion(deviceId, request);
                return deviceVersionResponse.getBody();
            });
        } catch (Exception ex) {
            log.error("RollbackDeviceVersion fail: {}", ex.getMessage(), ex);
            throw new DeviceServiceRollbackException(ex);
        }
    }

}
