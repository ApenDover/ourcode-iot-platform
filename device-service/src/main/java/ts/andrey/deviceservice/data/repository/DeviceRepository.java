package ts.andrey.deviceservice.data.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ts.andrey.deviceservice.data.entity.DeviceEntity;
import ts.andrey.dto.DeviceStatus;

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
                                    @Param("appName") String appName,
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
                             @Param("appName") String appName,
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
                             @Param("appName") String appName,
                             @Param("deviceType") String deviceType);

    @Modifying
    @Transactional
    @Query(value = """
                UPDATE DeviceEntity d
                   SET d.application = :appName,
                       d.etag = :etag,
                       d.status = :status,
                       d.version = :version
                 WHERE d.deviceId = :deviceId
            """, nativeQuery = true)
    int updateAndrey(@Param("deviceId") String deviceId,
                             @Param("appName") String appName,
                             @Param("etag") Long etag,
                             @Param("status") DeviceStatus status,
                             @Param("version") String version
                             );

}
