package ts.andrey.devicecollector.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ts.andrey.devicecollector.data.entity.DeviceEntity;

public interface DeviceRepository extends JpaRepository<DeviceEntity, String> {

}
