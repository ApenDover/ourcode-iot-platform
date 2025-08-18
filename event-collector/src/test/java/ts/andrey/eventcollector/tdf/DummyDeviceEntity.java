package ts.andrey.eventcollector.tdf;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ts.andrey.eventcollector.cassandra.entity.DeviceEntity;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class DummyDeviceEntity {

    public DeviceEntity getDefault() {
        final var deviceEntity = new DeviceEntity();
        deviceEntity.setDeviceId("deviceId");
        return deviceEntity;
    }

    public DeviceEntity getDefault(int i) {
        final var deviceEntity = new DeviceEntity();
        deviceEntity.setDeviceId("deviceId-" + i);
        return deviceEntity;
    }

    public List<DeviceEntity> getList(int j) {
        final var list = new ArrayList<DeviceEntity>();
        for (int i = 0; i < j; i++) {
            final var deviceEntity = new DeviceEntity();
            deviceEntity.setDeviceId("deviceId-" + i);
            list.add(deviceEntity);
        }
        return list;
    }

}
