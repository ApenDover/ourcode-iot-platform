package ts.andrey.failedeventsprocessor.tdf;

import com.nashkod.avro.DeviceEvent;
import com.nashkod.avro.EventType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class DummyDeviceEvent {

    public DeviceEvent getDefault() {
        return new DeviceEvent(
                "c9a646d3-9c61-4cb7-b8cd-6f3b5e3d0f7a",
                Instant.ofEpochMilli(125L), EventType.TEMPERATURE,
                "10", DummyTDF.device.getDefault()
        );
    }

    public DeviceEvent getInvalid() {
        return new DeviceEvent(
                "c9a646d3-9c61-4cb7-b8cd-6f3b5e3d0f7a",
                Instant.ofEpochMilli(125L), EventType.TEMPERATURE,
                "", DummyTDF.device.getDefault()
        );
    }

    public DeviceEvent getRandomEventId(int i) {
        return new DeviceEvent(
                UUID.randomUUID().toString(),
                Instant.ofEpochMilli(125L),
                EventType.TEMPERATURE, String.valueOf(i),
                DummyTDF.device.getDefault(i)
        );
    }

    public List<DeviceEvent> getList(int size) {
        final var list = new ArrayList<DeviceEvent>();
        for (int i = 0; i < size; i++) {
            list.add(getRandomEventId(i));
        }
        return list;
    }

}
