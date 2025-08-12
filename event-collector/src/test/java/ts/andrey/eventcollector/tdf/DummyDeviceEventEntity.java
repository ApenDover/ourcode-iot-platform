package ts.andrey.eventcollector.tdf;

import com.nashkod.avro.EventType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ts.andrey.eventcollector.cassandra.entity.DeviceEventEntity;
import ts.andrey.eventcollector.cassandra.entity.DeviceEventKey;

import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class DummyDeviceEventEntity {

    public DeviceEventEntity getDefault() {
        final var deviceEventEntity = new DeviceEventEntity();
        deviceEventEntity.setKey(getKey());
        deviceEventEntity.setType(EventType.TEMPERATURE);
        deviceEventEntity.setPayload("payload");
        return deviceEventEntity;
    }

    public DeviceEventKey getKey() {
        final var deviceEventKey = new DeviceEventKey();
        deviceEventKey.setDeviceId("deviceIdKey");
        deviceEventKey.setEventId(UUID.fromString("892dd1da-6f3f-49bc-a60d-a2b282d6efd0"));
        deviceEventKey.setTimestamp(897L);
        return deviceEventKey;
    }

}
