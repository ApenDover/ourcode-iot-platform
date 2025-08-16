package ts.andrey.devicecollector.postgres.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "t_device")
public class DeviceEntity {

    @Id
    private String deviceId;

    private String deviceType;

    private Long createdAt;

    private String meta;

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        DeviceEntity that = (DeviceEntity) object;
        return Objects.equals(deviceId, that.deviceId)
                && Objects.equals(deviceType, that.deviceType)
                && Objects.equals(createdAt, that.createdAt)
                && Objects.equals(meta, that.meta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceId, deviceType, createdAt, meta);
    }

}
