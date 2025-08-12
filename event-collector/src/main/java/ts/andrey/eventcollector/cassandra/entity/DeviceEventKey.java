package ts.andrey.eventcollector.cassandra.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

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
    private long timestamp;

    @PrimaryKeyColumn(
            name = "event_id",
            type = PrimaryKeyType.CLUSTERED,
            ordering = Ordering.DESCENDING
    )
    private UUID eventId;

}
