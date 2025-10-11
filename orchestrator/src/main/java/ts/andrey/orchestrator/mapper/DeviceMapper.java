package ts.andrey.orchestrator.mapper;

import org.mapstruct.Mapper;
import ts.andrey.orchestrator.dto.Device;
import ts.andrey.orchestrator.dto.DeviceCreateRequest;
import ts.andrey.orchestrator.dto.DeviceUpdateRequest;

@Mapper(componentModel = "spring")
public interface DeviceMapper {

    DeviceCreateRequest toOrchestratorCreateRequestDto(ts.andrey.device.model.DeviceCreateRequest deviceCreateRequest);

    ts.andrey.device.model.DeviceCreateRequest toDeviceCreateRequestDto(DeviceCreateRequest deviceCreateRequest);

    DeviceUpdateRequest toOrchestratorUpdateRequestDto(ts.andrey.device.model.DeviceUpdateRequest deviceUpdateRequest);

    ts.andrey.device.model.DeviceUpdateRequest toDeviceUpdateRequestDto(DeviceUpdateRequest deviceUpdateRequest);

    Device toOrchestratorDeviceDto(ts.andrey.device.model.Device device);

    ts.andrey.device.model.Device toDeviceDto(Device device);

}
