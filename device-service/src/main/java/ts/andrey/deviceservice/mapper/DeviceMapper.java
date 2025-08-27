package ts.andrey.deviceservice.mapper;

import com.github.f4b6a3.ulid.UlidCreator;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ts.andrey.deviceservice.data.entity.DeviceEntity;
import ts.andrey.dto.Device;
import ts.andrey.dto.DeviceCreateRequest;

import java.time.Instant;
import java.util.UUID;


@Mapper(componentModel = "spring",
        imports = {
                UUID.class,
                Instant.class,
                UlidCreator.class
        })
public interface DeviceMapper {

    @Mapping(target = "id", expression = "java(UUID.randomUUID())")
    @Mapping(target = "createdAt", expression = "java(Instant.ofEpochSecond(device.getCreatedAt()))")
    DeviceEntity toEntity(Device device);

    @Mapping(target = "createdAt", expression = "java(deviceEntity.getCreatedAt().toEpochMilli())")
    Device toDevice(DeviceEntity deviceEntity);

    @Mapping(target = "deviceId", expression = "java(UlidCreator.getUlid().toString())")
    Device createDevice(DeviceCreateRequest deviceRequest);

}
