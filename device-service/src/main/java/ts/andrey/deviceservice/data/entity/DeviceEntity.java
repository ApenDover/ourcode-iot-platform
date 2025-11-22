package ts.andrey.deviceservice.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.NaturalId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import ts.andrey.dto.DeviceStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "t_device")
@DynamicUpdate
@EntityListeners(AuditingEntityListener.class)
public class DeviceEntity {

    @Id
    private UUID id;

    @NaturalId
    @Column(unique = true, nullable = false)
    private String deviceId;

    private String deviceType;

    @CreatedDate
    private Instant createdAt;

    private String version;

    private Long etag;

    private String application;

    @Enumerated(EnumType.STRING)
    private DeviceStatus status;

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
                + ", deviceId='" + deviceId
                + '\'' + ", deviceType='" + deviceType + '\''
                + ", createdAt=" + createdAt
                + ", meta='"
                + meta + '\'' + '}';
    }

}
