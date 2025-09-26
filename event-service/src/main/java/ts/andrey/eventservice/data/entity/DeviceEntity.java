package ts.andrey.eventservice.data.entity;

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
@Table("deviceIds")
public class DeviceEntity {

    @PrimaryKey
    private String deviceId;

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        final var that = (DeviceEntity) object;
        return Objects.equals(deviceId, that.deviceId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(deviceId);
    }

}
