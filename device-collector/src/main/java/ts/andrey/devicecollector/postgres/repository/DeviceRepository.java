package ts.andrey.devicecollector.postgres.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ts.andrey.devicecollector.postgres.entity.DeviceEntity;

public interface DeviceRepository extends JpaRepository<DeviceEntity, String> {


}
