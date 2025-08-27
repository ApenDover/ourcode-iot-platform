package ts.andrey.deviceservice.tdf;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ts.andrey.deviceservice.data.entity.DeviceEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class DummyDeviceEntity {

    public DeviceEntity getDefault() {
        final var deviceEntity = new DeviceEntity();
        deviceEntity.setDeviceId("deviceId");
        deviceEntity.setId(UUID.fromString("d9de56ed-1c60-4220-852b-8855e7c8c78c"));
        deviceEntity.setDeviceType("deviceType");
        deviceEntity.setMeta("meta");
        deviceEntity.setCreatedAt(Instant.ofEpochMilli(200L));
        return deviceEntity;
    }

    public DeviceEntity getDefault(int i) {
        final var deviceEntity = new DeviceEntity();
        deviceEntity.setDeviceId("deviceId-" + i);
        deviceEntity.setId(UUID.randomUUID());
        deviceEntity.setDeviceType("deviceType");
        deviceEntity.setMeta("meta");
        deviceEntity.setCreatedAt(Instant.ofEpochMilli(200L));
        return deviceEntity;
    }

    public List<DeviceEntity> getList(int j) {
        final var list = new ArrayList<DeviceEntity>();
        for (int i = 0; i < j; i++) {
            list.add(getDefault(i));
        }
        return list;
    }

}
