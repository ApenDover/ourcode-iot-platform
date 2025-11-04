package ts.andrey.deviceservice.tdf;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ts.andrey.deviceservice.data.entity.DeviceEntity;
import ts.andrey.dto.DeviceStatus;

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
        deviceEntity.setVersion("0.0.1");
        deviceEntity.setEtag(1L);
        deviceEntity.setStatus(DeviceStatus.READY);
        return deviceEntity;
    }

    public DeviceEntity getUpdatedTypeMeta() {
        DeviceEntity entity = new DeviceEntity();
        entity.setDeviceId("deviceId");
        entity.setDeviceType("updatedType");
        entity.setMeta("updatedMeta");
        return entity;
    }

    public DeviceEntity getUpdatedType() {
        DeviceEntity entity = new DeviceEntity();
        entity.setDeviceId("deviceId");
        entity.setDeviceType("updatedType");
        entity.setMeta("meta");
        return entity;
    }

    public DeviceEntity getUpdatedMeta() {
        DeviceEntity entity = new DeviceEntity();
        entity.setDeviceId("deviceId");
        entity.setDeviceType("deviceType");
        entity.setMeta("updatedMeta");
        return entity;
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
