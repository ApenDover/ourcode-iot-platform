package ts.andrey.devicecollector.integration;

import com.zaxxer.hikari.HikariDataSource;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import ts.andrey.devicecollector.BaseIntegrationTest;
import ts.andrey.devicecollector.data.entity.DeviceEntity;
import ts.andrey.devicecollector.tdf.DummyTDF;
import ts.andrey.devicecollector.testutils.DateUtil;
import ts.andrey.devicecollector.testutils.KafkaProducerUtil;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeviceCollectorProcessingIT extends BaseIntegrationTest {

    JdbcTemplate templateDsTwo;
    JdbcTemplate templateDsOne;

    @BeforeEach
    void setUp() {
        final var ds0 = new HikariDataSource();
        ds0.setJdbcUrl(postgres1.getJdbcUrl());
        ds0.setUsername(postgres1.getUsername());
        ds0.setPassword(postgres1.getPassword());
        ds0.setDriverClassName("org.postgresql.Driver");

        templateDsOne = new JdbcTemplate(ds0);

        final var ds1 = new HikariDataSource();
        ds1.setJdbcUrl(postgres2.getJdbcUrl());
        ds1.setUsername(postgres2.getUsername());
        ds1.setPassword(postgres2.getPassword());
        ds1.setDriverClassName("org.postgresql.Driver");

        templateDsTwo = new JdbcTemplate(ds1);
    }

    @Test
    @SneakyThrows
    void successCase() {
        // GIVEN
        final var deviceOne = DummyTDF.device.getForShardOne();
        final var deviceTwo = DummyTDF.device.getForShardTwo();
        final var toSend = List.of(deviceOne, deviceTwo);

        // WHEN
        toSend.forEach(message ->
                KafkaProducerUtil.sendMessage(
                        kafka.getBootstrapServers(), "deviceOne",
                        schemaRegistry.getFirstMappedPort(), message
                )
        );

        // THEN
        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(1000, MILLISECONDS)
                .untilAsserted(() -> {
                    final var result = assertDoesNotThrow(() -> deviceRepository.findAll());
                    assertFalse(result.isEmpty());
                    assertEquals(2, result.size());
                    final var entity = assertDoesNotThrow(() ->
                            result.stream()
                                    .filter(it -> it.getDeviceId().equals("idforshard0"))
                                    .findFirst()
                                    .orElseThrow()
                    );
                    assertEquals("idforshard0", entity.getDeviceId());
                    assertEquals("deviceType", entity.getDeviceType());
                    assertEquals("meta", entity.getMeta());
                    assertEquals(Instant.ofEpochMilli(300L), entity.getCreatedAt());
                });

        //GIVEN
        final var deviceOneUpdated = DummyTDF.device.getForShardOneUpdated();

        // WHEN
        KafkaProducerUtil.sendMessage(
                kafka.getBootstrapServers(),
                "deviceOne",
                schemaRegistry.getFirstMappedPort(),
                deviceOneUpdated
        );

        // THEN
        // WAS UPDATED
        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(1000, MILLISECONDS)
                .untilAsserted(() -> {
                    final var resultUpdate = assertDoesNotThrow(() -> deviceRepository.findAll());
                    assertFalse(resultUpdate.isEmpty());
                    assertEquals(2, resultUpdate.size());
                    final var entityUpdate = resultUpdate.get(0);
                    assertEquals("idforshard0", entityUpdate.getDeviceId());
                    assertEquals("deviceType", entityUpdate.getDeviceType());
                    assertEquals("updated", entityUpdate.getMeta());
                    assertEquals(Instant.ofEpochMilli(600L), entityUpdate.getCreatedAt());
                });

        // DEVICE ONE SAVED TO SHARD 0
        final var expectedFromShardOne = templateDsOne.queryForObject(
                "SELECT * FROM t_device WHERE device_id = ?",
                (rs, rowNum) -> new DeviceEntity(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("device_id"),
                        rs.getString("device_type"),
                        DateUtil.getFromString(rs.getString("created_at")),
                        rs.getString("meta")
                ),
                deviceOne.getDeviceId()
        );

        assertNotNull(expectedFromShardOne);
        assertEquals("idforshard0", expectedFromShardOne.getDeviceId());
        assertEquals("deviceType", expectedFromShardOne.getDeviceType());
        assertEquals("updated", expectedFromShardOne.getMeta());
        assertEquals(Instant.ofEpochMilli(600L), expectedFromShardOne.getCreatedAt());

        // DEVICE TWO NOT SAVED TO SHARD 0
        final var emptyResultExOne = assertThrows(EmptyResultDataAccessException.class,
                () -> templateDsOne.queryForObject(
                        "SELECT * FROM t_device WHERE device_id = ?",
                        (rs, rowNum) -> new DeviceEntity(
                                UUID.fromString(rs.getString("id")),
                                rs.getString("device_id"),
                                rs.getString("device_type"),
                                DateUtil.getFromString(rs.getString("created_at")),
                                rs.getString("meta")
                        ),
                        deviceTwo.getDeviceId()
                )
        );

        assertEquals("Incorrect result size: expected 1, actual 0", emptyResultExOne.getMessage());

        // DEVICE TWO SAVED TO SHARD 1
        final var expectedFromShardTwo = templateDsTwo.queryForObject(
                "SELECT * FROM t_device WHERE device_id = ?",
                (rs, rowNum) -> new DeviceEntity(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("device_id"),
                        rs.getString("device_type"),
                        DateUtil.getFromString(rs.getString("created_at")),
                        rs.getString("meta")
                ),
                deviceTwo.getDeviceId()
        );

        assertNotNull(expectedFromShardTwo);
        assertEquals("idforshard1", expectedFromShardTwo.getDeviceId());
        assertEquals("deviceTwoType", expectedFromShardTwo.getDeviceType());
        assertEquals("metaTwo", expectedFromShardTwo.getMeta());
        assertEquals(Instant.ofEpochMilli(900L), expectedFromShardTwo.getCreatedAt());

        // DEVICE ONE NOT SAVED TO SHARD 1
        final var emptyResultExTwo = assertThrows(EmptyResultDataAccessException.class, () ->
                templateDsTwo.queryForObject(
                        "SELECT * FROM t_device WHERE device_id = ?",
                        (rs, rowNum) -> new DeviceEntity(
                                UUID.fromString(rs.getString("id")),
                                rs.getString("device_id"),
                                rs.getString("device_type"),
                                DateUtil.getFromString(rs.getString("created_at")),
                                rs.getString("meta")
                        ),
                        deviceOne.getDeviceId()
                )
        );

        assertEquals("Incorrect result size: expected 1, actual 0", emptyResultExTwo.getMessage());

        final var counter = Objects.requireNonNull(meterRegistry.find("device.postgres.success")
                        .counter())
                .count();

        assertEquals(2.0, counter);
    }

}
