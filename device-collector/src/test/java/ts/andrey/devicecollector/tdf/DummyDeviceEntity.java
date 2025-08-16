package ts.andrey.devicecollector.tdf;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ts.andrey.devicecollector.postgres.entity.DeviceEntity;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class DummyDeviceEntity {

    public DeviceEntity getDefault() {
        return new DeviceEntity("deviceId", "deviceType", 300L, "meta");
    }

    public DeviceEntity getDefault(int i) {
        return new DeviceEntity("deviceId-" + i, "deviceType", 300L, "meta");
    }

}
