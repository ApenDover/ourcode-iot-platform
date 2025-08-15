package ts.andrey.eventcollector.cassandra.entity;

import com.nashkod.avro.EventType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Table("device_events")
public class DeviceEventEntity {

    @PrimaryKey
    private DeviceEventKey key;

    private EventType type;

    private String payload;

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        DeviceEventEntity that = (DeviceEventEntity) object;
        return Objects.equals(key, that.key)
                && type == that.type
                && Objects.equals(payload, that.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, type, payload);
    }

}
