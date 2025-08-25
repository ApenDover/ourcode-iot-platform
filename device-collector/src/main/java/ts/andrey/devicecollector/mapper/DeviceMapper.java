package ts.andrey.devicecollector.mapper;

import com.nashkod.avro.Device;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ts.andrey.devicecollector.data.entity.DeviceEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DeviceMapper {

    @Mapping(target = "id", ignore = true)
    DeviceEntity toDeviceEntity(Device device);

    Device toDevice(DeviceEntity device);

    List<DeviceEntity> toDeviceEntityList(List<Device> device);

    List<Device> toDeviceList(List<DeviceEntity> saved);

}
