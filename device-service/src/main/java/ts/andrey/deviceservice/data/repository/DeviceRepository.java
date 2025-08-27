package ts.andrey.deviceservice.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ts.andrey.deviceservice.data.entity.DeviceEntity;

public interface DeviceRepository extends JpaRepository<DeviceEntity, String> {

}
