package ts.andrey.devicecollector.tdf;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ts.andrey.devicecollector.postgres.entity.DeviceEntity;

import java.time.Instant;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class DummyDeviceEntity {

    public DeviceEntity getDefault() {
        return DeviceEntity.builder()
                .deviceId("deviceId")
                .meta("meta")
                .deviceType("deviceType")
                .createdAt(Instant.ofEpochMilli(300L))
                .build();
    }

    public DeviceEntity getDefault(int i) {
        return DeviceEntity.builder()
                .deviceId("deviceId-" + i)
                .meta("meta")
                .deviceType("deviceType")
                .createdAt(Instant.ofEpochMilli(300L))
                .build();
    }

}
