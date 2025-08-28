package ts.andrey.deviceservice.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "t_device")
@EntityListeners(AuditingEntityListener.class)
public class DeviceEntity {

    @Id
    private UUID id;

    @Column(unique = true, nullable = false)
    private String deviceId;

    private String deviceType;

    @CreatedDate
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
