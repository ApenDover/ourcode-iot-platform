package ts.andrey.devicecollector.postgres.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

}
