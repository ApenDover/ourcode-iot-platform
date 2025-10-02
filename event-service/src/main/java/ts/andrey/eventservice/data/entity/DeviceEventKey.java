package ts.andrey.eventservice.data.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@ToString
@PrimaryKeyClass
public class DeviceEventKey {

    @PrimaryKeyColumn(
            name = "device_id",
            type = PrimaryKeyType.PARTITIONED,
            ordering = Ordering.DESCENDING
    )
    private String deviceId;

    @PrimaryKeyColumn(
            name = "timestamp",
            type = PrimaryKeyType.CLUSTERED,
            ordering = Ordering.DESCENDING
    )
    private Long timestamp;

    @PrimaryKeyColumn(
            name = "event_id",
            type = PrimaryKeyType.CLUSTERED,
            ordering = Ordering.DESCENDING
    )
    private UUID eventId;

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        DeviceEventKey that = (DeviceEventKey) object;
        return Objects.equals(timestamp, that.timestamp)
                && Objects.equals(deviceId, that.deviceId)
                && Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceId, timestamp, eventId);
    }

}
