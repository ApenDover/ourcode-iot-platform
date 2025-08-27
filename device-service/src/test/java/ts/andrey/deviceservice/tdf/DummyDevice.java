package ts.andrey.deviceservice.tdf;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ts.andrey.dto.Device;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class DummyDevice {

    public Device getDefault() {
        final var device = new Device();
        device.setDeviceId("deviceId");
        device.setDeviceType("deviceType");
        device.setMeta("meta");
        device.setCreatedAt(600L);
        return device;
    }

    public Device getDefault(int i) {
        final var device = new Device();
        device.setDeviceId("deviceId" + i);
        device.setDeviceType("deviceType");
        device.setMeta("meta");
        device.setCreatedAt(600L);
        return device;
    }

    public List<Device> getList(int size) {
        final var list = new ArrayList<Device>();
        for (int i = 0; i < size; i++) {
            list.add(getDefault(i));
        }
        return list;
    }

}
