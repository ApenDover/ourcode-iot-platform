package ts.andrey.devicecollector.postgres.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Builder
@Getter
@Setter
@Entity
@Table(name = "tdevice")
@AllArgsConstructor
@NoArgsConstructor
public class DeviceEntity {

    @Id
    private UUID id;
    private Long deviceId;
    private String deviceType;
    private Instant createdAt;
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

}
