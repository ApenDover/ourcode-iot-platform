package ts.andrey.eventservice;

import com.datastax.oss.driver.api.core.CqlSession;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    protected static final String LOCALHOST_HTTP = "http://localhost:";

    @Autowired
    public RestTemplate restTemplate;

    @LocalServerPort
    protected int localPort;

    @Container
    protected static final GenericContainer<?> cassandra = new GenericContainer<>(
            DockerImageName.parse("cassandra:5.0"))
            .withStartupTimeout(Duration.ofMinutes(3))
            .withCreateContainerCmdModifier(cmd -> cmd.withName("cassandra"))
            .withNetwork(Network.SHARED)
            .withNetworkAliases("cassandra")
            .withExposedPorts(9042);


    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        String contactPoint = "localhost:" + cassandra.getMappedPort(9042);
        log.info("Cassandra contact point: {}", contactPoint);
        registry.add("spring.cassandra.contact-points", () -> contactPoint);
        registry.add("spring.cassandra.port", () -> cassandra.getMappedPort(9042));
        registry.add("spring.cassandra.local-datacenter", () -> "datacenter1");
    }

    protected <T> ResponseEntity<T> sendGet(String path, Class<T> responseType) {
        return restTemplate.exchange(getUrl(path), HttpMethod.GET, null, responseType);
    }

    protected String getUrl(String path) {
        return path.startsWith("/") ? baseUrl() + path : baseUrl() + "/" + path;
    }

    private String baseUrl() {
        return LOCALHOST_HTTP + localPort;
    }

    @SneakyThrows
    @BeforeAll
    static void startContainers() {
        try (CqlSession session = CqlSession.builder()
                .addContactPoint(
                        new InetSocketAddress(cassandra.getHost(), cassandra.getFirstMappedPort()))
                .withLocalDatacenter("datacenter1")
                .build()) {
            final var resource = new ClassPathResource("init.cql");
            if (resource.exists()) {
                String cql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
                String[] statements = cql.split(";");

                for (String statement : statements) {
                    String trimmed = statement.trim();
                    if (!trimmed.isEmpty()) {
                        session.execute(trimmed);
                    }
                }
                log.info("init.cql executed successfully");
            } else {
                log.info("init.cql not found, skipping");
            }
        }
    }

}
