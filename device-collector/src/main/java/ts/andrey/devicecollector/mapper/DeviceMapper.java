package ts.andrey.devicecollector.mapper;

import com.nashkod.avro.Device;
import org.mapstruct.Mapper;
import ts.andrey.devicecollector.postgres.entity.DeviceEntity;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DeviceMapper {

    DeviceEntity toDeviceEntity(Device device);

    Device toDevice(DeviceEntity device);

    List<DeviceEntity> toDeviceEntityList(List<Device> device);

    List<Device> toDeviceList(List<DeviceEntity> saved);

}
