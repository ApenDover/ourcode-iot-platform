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

    public DeviceEntity getDeviceOne() {
        final var deviceEntity = new DeviceEntity();
        deviceEntity.setId(UUID.fromString("f4c1b0dd-0e4b-4d91-95b0-92d3f5ffad78"));
        deviceEntity.setDeviceId("DEV-001");
        deviceEntity.setDeviceType("SENSOR");
        deviceEntity.setCreatedAt(Instant.parse("2025-08-27T20:45:12.345678Z"));
        deviceEntity.setMeta("meta-text");
        deviceEntity.setEtag(0L);
        deviceEntity.setVersion("0.0.1");
        deviceEntity.setApplication("device-service");
        deviceEntity.setStatus(DeviceStatus.READY);
        return deviceEntity;
    }

    public DeviceEntity getDeviceTwo() {
        final var deviceEntity = new DeviceEntity();
        deviceEntity.setId(UUID.fromString("c0a801d9-77ab-4c3e-bcb2-0c5ffb97ea21"));
        deviceEntity.setDeviceId("DEV-002");
        deviceEntity.setDeviceType("SENSOR");
        deviceEntity.setCreatedAt(Instant.parse("2025-08-28T07:10:12.345678Z"));
        deviceEntity.setMeta("meta-text-two");
        deviceEntity.setEtag(0L);
        deviceEntity.setVersion("0.0.1");
        deviceEntity.setApplication("device-service");
        deviceEntity.setStatus(DeviceStatus.READY);
        return deviceEntity;
    }

    public DeviceEntity getDeviceDelete() {
        final var deviceEntity = new DeviceEntity();
        deviceEntity.setId(UUID.fromString("2a1f9b92-3c9d-497b-a84c-efc11cbeb3a2"));
        deviceEntity.setDeviceId("DEV-DELETE");
        deviceEntity.setDeviceType("SENSOR");
        deviceEntity.setCreatedAt(Instant.parse("2025-08-28T07:15:12.345678Z"));
        deviceEntity.setMeta("meta-text-for-delete");
        deviceEntity.setEtag(0L);
        deviceEntity.setVersion("0.0.1");
        deviceEntity.setApplication("device-service");
        deviceEntity.setStatus(DeviceStatus.READY);
        return deviceEntity;
    }

    public List<DeviceEntity> getAllTestDevices() {
        return List.of(
                getDeviceOne(),
                getDeviceTwo(),
                getDeviceDelete()
        );
    }

}
