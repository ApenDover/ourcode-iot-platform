package ts.andrey.eventcollector.tdf;

import com.nashkod.avro.Device;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class DummyDevice {

    public Device getDefault() {
        return new Device("deviceId");
    }

}
