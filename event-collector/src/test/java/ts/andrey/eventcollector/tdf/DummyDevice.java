package ts.andrey.eventcollector.tdf;

import com.nashkod.avro.Device;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class DummyDevice {

    public Device getDefault() {
        return new Device("deviceId", "deviceType", 300L, "meta");
    }

    public Device getDefault(int i) {
        return new Device("deviceId-" + i, "deviceType", 300L, "meta");
    }

    public List<Device> getList(int size) {
        final var list = new ArrayList<Device>();
        for (int i = 0; i < size; i++) {
            list.add(getDefault(i));
        }
        return list;
    }

}
