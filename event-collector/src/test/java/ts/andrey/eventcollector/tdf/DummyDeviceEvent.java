package ts.andrey.eventcollector.tdf;

import com.nashkod.avro.DeviceEvent;
import com.nashkod.avro.EventType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class DummyDeviceEvent {

    public DeviceEvent getDefault() {
        return new DeviceEvent("eventId", "deviceId", 125L, EventType.TEMPERATURE, "10");
    }

}
