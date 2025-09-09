package ts.andrey.failedeventsprocessor.integration;

import com.nashkod.avro.DeviceError;
import com.nashkod.avro.DeviceEventError;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import ts.andrey.failedeventsprocessor.BaseIntegrationTest;
import ts.andrey.failedeventsprocessor.tdf.DummyTDF;
import ts.andrey.failedeventsprocessor.testutils.KafkaProducerUtil;
import ts.andrey.failedeventsprocessor.testutils.MinioFileReader;
import ts.andrey.failedeventsprocessor.utils.MinioNameGenerator;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReadAndSaveIT extends BaseIntegrationTest {

    @Value("${spring.kafka.template.device.dlt}")
    private String dltDeviceTopic;

    @Value("${spring.kafka.template.events.dlt}")
    private String dltEventsTopic;

    @Value("${minio.bucket-name}")
    private String bucket;

    @SneakyThrows
    @Test
    void successDeviceErrorCase() {
        // GIVEN
        final var message = DummyTDF.deviceError.getDefault();
        final var fileName = MinioNameGenerator.generateName(
                message.getReceivedAt(),
                message.getErrorMeta().getErrorSource().name()
        );

        // WHEN
        KafkaProducerUtil.sendMessage(
                kafka.getBootstrapServers(),
                dltDeviceTopic,
                schemaRegistry.getFirstMappedPort(),
                message
        );

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    // THEN
                    final var stringFromMinio = assertDoesNotThrow(() ->
                            MinioFileReader.getFile(minioClient, bucket, fileName)
                    );
                    assertNotNull(stringFromMinio);
                    final var deviceError = objectMapper.readValue(stringFromMinio, DeviceError.class);
                    assertThat(deviceError)
                            .usingRecursiveComparison()
                            .isEqualTo(message);
                });
    }

    @SneakyThrows
    @Test
    void successEventErrorCase() {
        // GIVEN
        final var message = DummyTDF.deviceEventError.getDefault();
        final var fileName = MinioNameGenerator.generateName(
                message.getReceivedAt(),
                message.getErrorMeta().getErrorSource().name()
        );

        // WHEN
        KafkaProducerUtil.sendMessage(
                kafka.getBootstrapServers(),
                dltEventsTopic,
                schemaRegistry.getFirstMappedPort(),
                message
        );

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    // THEN
                    final var stringFromMinio = assertDoesNotThrow(() ->
                            MinioFileReader.getFile(minioClient, bucket, fileName)
                    );
                    assertNotNull(stringFromMinio);
                    final var deviceError = objectMapper.readValue(stringFromMinio, DeviceEventError.class);
                    assertThat(deviceError)
                            .usingRecursiveComparison()
                            .isEqualTo(message);
                });
    }

}
