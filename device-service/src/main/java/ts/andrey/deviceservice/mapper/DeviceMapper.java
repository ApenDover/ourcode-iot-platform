package ts.andrey.deviceservice.mapper;

import com.github.f4b6a3.ulid.UlidCreator;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ts.andrey.deviceservice.data.entity.DeviceEntity;
import ts.andrey.dto.Device;
import ts.andrey.dto.DeviceCreateRequest;
import ts.andrey.dto.DeviceVersionResponse;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


@Mapper(componentModel = "spring",
        imports = {
                Objects.class,
                UUID.class,
                Instant.class,
                UlidCreator.class
        })
public interface DeviceMapper {

    @Mapping(target = "id", expression = "java(UUID.randomUUID())")
    @Mapping(target = "createdAt", expression = "java(Objects.isNull(device.getCreatedAt()) "
            + "? null : Instant.ofEpochMilli(device.getCreatedAt()))")
    @Mapping(target = "application", ignore = true)
    DeviceEntity toEntity(Device device);

    @Mapping(target = "createdAt", expression = "java(deviceEntity.getCreatedAt().toEpochMilli())")
    @Mapping(target = "etag", expression = "java(deviceEntity.getEtag() == null ? 0L : deviceEntity.getEtag())")
    Device toDevice(DeviceEntity deviceEntity);

    List<Device> toDevices(List<DeviceEntity> deviceEntities);

    @Mapping(target = "deviceId", expression = "java(UlidCreator.getUlid().toString())")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "status", expression = "java(ts.andrey.dto.DeviceStatus.READY)")
    @Mapping(target = "etag", expression = "java(Long.valueOf(0))")
    Device createDevice(DeviceCreateRequest deviceRequest);

    @Mapping(target = "deviceId", expression = "java(UlidCreator.getUlid().toString())")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "id", expression = "java(UUID.randomUUID())")
    @Mapping(target = "etag", expression = "java(Long.valueOf(0))")
    @Mapping(target = "status", expression = "java(ts.andrey.dto.DeviceStatus.READY)")
    DeviceEntity createDeviceEntity(DeviceCreateRequest deviceRequest);

    @Mapping(target = "prevVersion", expression = "java(oldVersion)")
    @Mapping(target = "targetVersion", source = "device.version")
    @Mapping(target = "status", source = "status")
    DeviceVersionResponse toUpdateVersionResponse(DeviceEntity device, @Context String oldVersion);

    @Mapping(target = "prevVersion", expression = "java(oldVersion)")
    @Mapping(target = "targetVersion", source = "device.version")
    @Mapping(target = "status", source = "status")
    DeviceVersionResponse toUpdateVersionResponse(Device device, @Context String oldVersion);
}
