package ts.andrey.devicecollector.integration;

import com.nashkod.avro.Device;
import com.zaxxer.hikari.HikariDataSource;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ts.andrey.devicecollector.BaseIntegrationTest;
import ts.andrey.devicecollector.data.repository.DeviceBatchRepository;
import ts.andrey.devicecollector.tdf.DummyTDF;
import ts.andrey.devicecollector.testutils.KafkaConsumerUtil;
import ts.andrey.devicecollector.testutils.KafkaProducerUtil;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

class DeviceCollectorWithMockProcessingIT extends BaseIntegrationTest {

    JdbcTemplate templateDsTwo;
    JdbcTemplate templateDsOne;

    @MockitoBean
    DeviceBatchRepository deviceBatchRepository;

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
    void dataBaseFailCase() {
        // GIVEN
        final var deviceOne = DummyTDF.device.getForShardOne();
        final var deviceTwo = DummyTDF.device.getForShardTwo();
        final var toSend = List.of(deviceOne, deviceTwo);
        when(deviceBatchRepository.batchUpsert(anyList()))
                .thenThrow(new DataIntegrityViolationException("Simulated DB error"));

        // WHEN
        toSend.forEach(message ->
                KafkaProducerUtil.sendMessage(
                        kafka.getBootstrapServers(), "deviceOne",
                        schemaRegistry.getFirstMappedPort(), message
                )
        );

        // THEN
        await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(1000, MILLISECONDS)
                .untilAsserted(() -> {
                    final var records = KafkaConsumerUtil.getMessages(kafka.getBootstrapServers(), "dltDeviceOne",
                            "group", schemaRegistry.getFirstMappedPort(), Device.class);
                    assertNotNull(records);
                    assertFalse(records.isEmpty());
                    assertEquals(2, records.count());
                });
    }

}
