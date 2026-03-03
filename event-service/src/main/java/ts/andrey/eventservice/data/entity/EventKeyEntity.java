package ts.andrey.eventservice.data.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Table("device_events_key")
public class EventKeyEntity {

    @PrimaryKey
    private EventKeyEntityKey key;

    private Long timestamp;

    @Column("device_id")
    private String deviceId;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        EventKeyEntity that = (EventKeyEntity) o;
        return Objects.equals(key, that.key) && Objects.equals(timestamp, that.timestamp) && Objects.equals(deviceId, that.deviceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, timestamp, deviceId);
    }

}
