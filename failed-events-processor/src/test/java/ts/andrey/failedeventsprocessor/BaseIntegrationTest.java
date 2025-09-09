package ts.andrey.failedeventsprocessor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    private static final String LOCALHOST = "localhost:";
    private static final String LOCALHOST_HTTP = "http://localhost:";

    @Autowired
    public MinioClient minioClient;

    @Autowired
    public ObjectMapper objectMapper;

    @SuppressWarnings("resource")
    @Container
    protected static final GenericContainer<?> zookeeper = new GenericContainer<>(
            DockerImageName.parse("bitnami/zookeeper:latest"))
            .withCreateContainerCmdModifier(cmd -> cmd.withName("zookeeper"))
            .withNetwork(Network.SHARED)
            .withNetworkAliases("int")
            .withExposedPorts(2181)
            .withEnv("ALLOW_ANONYMOUS_LOGIN", "yes");


    @Container
    protected static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka-native:3.8.0"))
            .withCreateContainerCmdModifier(cmd -> cmd.withName("kafka"))
            .withNetwork(Network.SHARED)
            .withNetworkAliases("int")
            .withExposedPorts(9092, 9093)
            .withEnv("KAFKA_CFG_ZOOKEEPER_CONNECT", "zookeeper:2181")
            .withEnv("ALLOW_PLAINTEXT_LISTENER", "yes")
            .withEnv("KAFKA_CFG_LISTENERS", "PLAINTEXT://0.0.0.0:9092,PLAINTEXT_INTERNAL://0.0.0.0:9093")
            .withEnv("KAFKA_CFG_ADVERTISED_LISTENERS", "PLAINTEXT://localhost:9092,PLAINTEXT_INTERNAL://kafka:9093")
            .dependsOn(zookeeper);

    @SuppressWarnings("resource")
    @Container
    protected static final GenericContainer<?> schemaRegistry = new GenericContainer<>(
            DockerImageName.parse("confluentinc/cp-schema-registry:7.5.0"))
            .withCreateContainerCmdModifier(cmd -> cmd.withName("schema-registry"))
            .withNetwork(Network.SHARED)
            .withNetworkAliases("int")
            .withExposedPorts(8081)
            .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
            .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://kafka:9093")
            .dependsOn(kafka);

    @SuppressWarnings("resource")
    @Container
    protected static final GenericContainer<?> minio = new GenericContainer<>(
            DockerImageName.parse("minio/minio:latest"))
            .withStartupTimeout(Duration.ofMinutes(1))
            .withCreateContainerCmdModifier(cmd -> cmd.withName("minio"))
            .withNetwork(Network.SHARED)
            .withEnv("MINIO_ROOT_USER", "minio")
            .withEnv("MINIO_ROOT_PASSWORD", "password")
            .withEnv("MINIO_PROMETHEUS_AUTH_TYPE", "public")
            .withCommand("server", "/data", "--console-address", ":9001")
            .withExposedPorts(9000);


    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("kafka.bootstrap.servers", () -> LOCALHOST + kafka.getFirstMappedPort());
        registry.add("schema.registry.url", () -> LOCALHOST_HTTP + schemaRegistry.getFirstMappedPort());
        registry.add("minio.url", () -> LOCALHOST_HTTP + minio.getFirstMappedPort());
    }

}
