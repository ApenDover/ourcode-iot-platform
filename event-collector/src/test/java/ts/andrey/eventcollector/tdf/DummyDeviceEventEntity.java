package ts.andrey.eventcollector.tdf;

import com.nashkod.avro.EventType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ts.andrey.eventcollector.cassandra.entity.DeviceEventEntity;
import ts.andrey.eventcollector.cassandra.entity.DeviceEventKey;

import java.util.ArrayList;
import java.util.List;
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

    public List<DeviceEventEntity> getList(int j) {
        final var list = new ArrayList<DeviceEventEntity>();
        for (int i = 0; i < j; i++) {
            final var deviceEventEntity = new DeviceEventEntity();
            deviceEventEntity.setKey(getRandomKey(i));
            deviceEventEntity.setType(EventType.TEMPERATURE);
            deviceEventEntity.setPayload("payload-" + i);
            list.add(deviceEventEntity);
        }
        return list;
    }

    public DeviceEventKey getKey() {
        final var deviceEventKey = new DeviceEventKey();
        deviceEventKey.setDeviceId("deviceIdKey");
        deviceEventKey.setEventId(UUID.fromString("892dd1da-6f3f-49bc-a60d-a2b282d6efd0"));
        deviceEventKey.setTimestamp(897L);
        return deviceEventKey;
    }

    public DeviceEventKey getRandomKey(int i) {
        final var deviceEventKey = new DeviceEventKey();
        deviceEventKey.setDeviceId("deviceIdKey-" + i);
        deviceEventKey.setEventId(UUID.randomUUID());
        deviceEventKey.setTimestamp(431L);
        return deviceEventKey;
    }

}
