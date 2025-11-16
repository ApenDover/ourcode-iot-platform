package ts.andrey.deviceservice.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ts.andrey.deviceservice.data.entity.DeviceEntity;

import java.util.Optional;

public interface DeviceRepository extends JpaRepository<DeviceEntity, String> {

    Optional<DeviceEntity> findByDeviceId(String deviceId);

    @Transactional
    @Modifying
    @Query("delete from DeviceEntity d where d.deviceId = :deviceId")
    int deleteByDeviceId(@Param("deviceId") String deviceId);

    @Modifying
    @Transactional
    @Query("""
                UPDATE DeviceEntity d
                   SET d.deviceType = :deviceType,
                       d.meta = :meta,
                       d.application = :appName
                 WHERE d.deviceId = :deviceId
            """)
    int updateTypeAndMetaByDeviceId(@Param("deviceId") String deviceId,
                                    @Param("deviceType") String deviceType,
                                    @Param("application") String appName,
                                    @Param("meta") String meta);

    @Modifying
    @Transactional
    @Query("""
                UPDATE DeviceEntity d
                   SET d.meta = :meta,
                       d.application = :appName
                 WHERE d.deviceId = :deviceId
            """)
    int updateMetaByDeviceId(@Param("deviceId") String deviceId,
                             @Param("application") String appName,
                             @Param("meta") String meta);

    @Modifying
    @Transactional
    @Query("""
                UPDATE DeviceEntity d
                   SET d.deviceType = :deviceType,
                       d.application = :appName
                 WHERE d.deviceId = :deviceId
            """)
    int updateTypeByDeviceId(@Param("deviceId") String deviceId,
                             @Param("application") String appName,
                             @Param("deviceType") String deviceType);

}
