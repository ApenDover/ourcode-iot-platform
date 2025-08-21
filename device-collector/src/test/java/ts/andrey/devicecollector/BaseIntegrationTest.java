package ts.andrey.devicecollector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import ts.andrey.devicecollector.postgres.repository.DeviceRepository;

import javax.sql.DataSource;

@Slf4j
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.liquibase.enabled=false",
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
                "spring.datasource.driver-class-name=org.apache.shardingsphere.driver.ShardingSphereDriver"
        }
)
@Testcontainers
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    private static final String LOCALHOST = "localhost:";
    private static final String LOCALHOST_HTTP = "http://localhost:";

    @Autowired
    public DynamicPropertyRegistry registry;

    @Autowired
    public DeviceRepository deviceRepository;

    @Autowired
    public PostgreSQLContainer<?> postgres1;

    @Autowired
    public PostgreSQLContainer<?> postgres2;

    @Container
    protected static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka-native:3.8.0"))
            .withCreateContainerCmdModifier(cmd -> cmd.withName("kafka"))
            .withNetwork(Network.SHARED)
            .withNetworkAliases("int")
            .withExposedPorts(9092, 9093)
            .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
            .withEnv("ALLOW_PLAINTEXT_LISTENER", "yes")
            .withEnv("KAFKA_CFG_LISTENERS", "PLAINTEXT://0.0.0.0:9092,PLAINTEXT_INTERNAL://0.0.0.0:9093")
            .withEnv("KAFKA_CFG_ADVERTISED_LISTENERS", "PLAINTEXT://localhost:9092,PLAINTEXT_INTERNAL://kafka:9093");


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


    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("kafka.bootstrap.servers", kafka::getBootstrapServers);
        registry.add("schema.registry.url", () -> LOCALHOST_HTTP + schemaRegistry.getFirstMappedPort());
    }

}
