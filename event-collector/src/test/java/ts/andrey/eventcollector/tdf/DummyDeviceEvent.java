package ts.andrey.eventcollector.tdf;

import com.nashkod.avro.DeviceEvent;
import com.nashkod.avro.EventType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class DummyDeviceEvent {

    public DeviceEvent getDefault() {
        return new DeviceEvent(
                "c9a646d3-9c61-4cb7-b8cd-6f3b5e3d0f7a",
                "deviceId", 125L,
                EventType.TEMPERATURE, "10"
        );
    }

}
