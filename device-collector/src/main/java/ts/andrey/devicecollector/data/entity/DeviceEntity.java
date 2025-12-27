package ts.andrey.devicecollector.data.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Builder
@Getter
@Setter
@Entity
@Table(name = "t_device")
@AllArgsConstructor
@NoArgsConstructor
public class DeviceEntity {

    @Id
    @Column(updatable = false)
    private UUID id;

    @Column(updatable = false, unique = true, nullable = false)
    private String deviceId;

    private String deviceType;

    private Instant createdAt;

    private String version;

    private Long etag;

    private String application;

    @JsonIgnore
    private String status;

    private String meta;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DeviceEntity)) {
            return false;
        }
        return deviceId != null && deviceId.equals(((DeviceEntity) o).getDeviceId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "DeviceEntity{"
                + "id=" + id
                + ", deviceId='" + deviceId + '\''
                + ", deviceType='" + deviceType + '\''
                + ", createdAt=" + createdAt
                + ", version='" + version + '\''
                + ", etag=" + etag
                + ", application='" + application + '\''
                + ", status=" + status
                + ", meta='" + meta + '\''
                + '}';
    }

}
