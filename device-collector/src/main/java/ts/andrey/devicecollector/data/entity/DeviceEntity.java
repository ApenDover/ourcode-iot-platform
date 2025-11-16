package ts.andrey.devicecollector.data.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
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
@Table(name = "t_device")
@AllArgsConstructor
@NoArgsConstructor
public class DeviceEntity {

    @Id
    private UUID id;

    @Column(unique = true, nullable = false)
    private String deviceId;

    private String deviceType;

    private Instant createdAt;

    private String version;

    private Long etag;

    private String application;

    @JsonIgnore
    @Transient
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
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

}
