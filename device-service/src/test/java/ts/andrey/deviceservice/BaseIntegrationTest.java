package ts.andrey.deviceservice;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ts.andrey.deviceservice.data.repository.DeviceRepository;
import ts.andrey.deviceservice.utils.TestRestClient;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@Slf4j
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.flyway.enabled=false",
                "spring.jpa.hibernate.ddl-auto=none",
                "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
                "spring.datasource.driver-class-name=org.apache.shardingsphere.driver.ShardingSphereDriver"
        }
)
@Testcontainers
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    private static Network network = Network.newNetwork();

    private static final String LOCALHOST_HTTP = "http://localhost:";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("keycloak")
            .withCreateContainerCmdModifier(cmd -> cmd.withName("postgres"))
            .withUsername("test")
            .withPassword("test")
            .withNetwork(network);

    @Container
    static GenericContainer<?> keycloak;

    static {
        try {
            keycloak = new GenericContainer<>("quay.io/keycloak/keycloak:26.3")
                    .withNetwork(network)
                    .withCreateContainerCmdModifier(cmd -> cmd.withName("keycloak"))
                    .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
                    .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin123")
                    .withEnv("KC_DB", "postgres")
                    .withEnv("KC_DB_URL_HOST", "postgres")
                    .withEnv("KC_DB_URL_PORT", "5432")
                    .withEnv("KC_DB_URL_DATABASE", "keycloak")
                    .withEnv("KC_DB_USERNAME", "test")
                    .withEnv("KC_DB_PASSWORD", "test")
                    .withCopyToContainer(Transferable.of(Files.readAllBytes(Paths.get("src/test/resources/realm-export.json"))), "/opt/keycloak/data/import/realm-export.json")
                    .withCommand("start-dev", "--import-realm")
                    .withExposedPorts(8080)
                    .withCreateContainerCmdModifier(cmd ->
                            cmd.withHostConfig(
                                    cmd.getHostConfig().withPortBindings(
                                            Arrays.asList(new PortBinding(Ports.Binding.bindPort(8081), new ExposedPort(8080)))
                                    )
                            )

                    ).dependsOn(postgres);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Container
    public static GenericContainer<?> redisContainer =
            new GenericContainer<>("redis:7-alpine")
                    .withExposedPorts(6379);

    @Autowired
    public RestTemplate restTemplate;

    @LocalServerPort
    public int localPort;

    @Autowired
    public MeterRegistry meterRegistry;

    @Autowired
    public DeviceRepository deviceRepository;

    @Autowired
    public PostgreSQLContainer<?> postgres1;

    @Autowired
    public PostgreSQLContainer<?> postgres2;

    public TestRestClient testRestClient;

    public String getUrl(String path) {
        if (path.startsWith("/")) {
            return baseUrl() + path;
        }
        return baseUrl() + "/" + path;
    }

    public String getUnsecuredUrl(String path) {
        String url = baseUnsecuredUrl() + path;
        Assertions.assertTrue(url.contains("http:"));
        return url;
    }

    public ResponseEntity<String> sendGet(String path, String jwtToken) {
        HttpHeaders headers = new HttpHeaders();
        if (jwtToken != null) {
            headers.setBearerAuth(jwtToken);
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(getUrl(path), HttpMethod.GET, entity, String.class);
    }

    public ResponseEntity<String> sendGetHttpUnsecured(String path) {
        return restTemplate.getForEntity(getUnsecuredUrl(path), String.class);
    }

    @SneakyThrows
    public ResponseEntity<Object> sendPost(String path, Object object) {
        HttpHeaders headers = new HttpHeaders();
        final var jwtToken = getAccessToken("admin", "admin123");
        if (jwtToken != null) {
            headers.setBearerAuth(jwtToken);
        }
        HttpEntity<Object> entity = new HttpEntity<>(object, headers);
        return restTemplate.postForEntity(getUrl(path), entity, Object.class);
    }

    @SneakyThrows
    public <Req, Res> ResponseEntity<Res> sendPost(String path, Req request,
                                                   Class<Res> responseClass) {
        final var jwtToken = getAccessToken("admin", "admin123");
        HttpHeaders headers = new HttpHeaders();
        if (jwtToken != null) {
            headers.setBearerAuth(jwtToken);
        }
        HttpEntity<Req> entity = new HttpEntity<>(request, headers);
        return restTemplate.postForEntity(getUrl(path), entity, responseClass);
    }

    public <Req, Res> Res sendPostOk(String url, Req request, Class<Res> responseClass, String jwtToken) {
        return sendPostOk(url, new HttpEntity<>(request), responseClass, jwtToken);
    }

    public <Res> Res sendPostOk(String url, HttpEntity<?> httpEntity, Class<Res> responseClass, String jwtToken) {
        final var actual = sendRequest(url, HttpMethod.POST, httpEntity, responseClass);
        assertEquals(HttpStatus.OK, actual.getStatusCode());
        final var body = actual.getBody();
        assertInstanceOf(responseClass, body);
        return body;
    }

    @SneakyThrows
    public <Res> ResponseEntity<Res> sendRequest(String url, HttpMethod method,
                                                 HttpEntity<?> body, Class<Res> responseType) {
        final var jwtToken = getAccessToken("admin", "admin123");
        HttpHeaders headers = new HttpHeaders();
        if (jwtToken != null) {
            headers.setBearerAuth(jwtToken);
        }

        HttpEntity<?> entity;
        if (body != null) {
            HttpHeaders combinedHeaders = new HttpHeaders();
            combinedHeaders.putAll(headers);
            combinedHeaders.putAll(body.getHeaders());
            entity = new HttpEntity<>(body.getBody(), combinedHeaders);
        } else {
            entity = new HttpEntity<>(headers);
        }

        return restTemplate.exchange(getUrl(url), method, entity, responseType);
    }

    public static String getAccessToken(String username, String password) throws Exception {
        String realm = "iot-platform";
        String clientId = "device-service";
        String secret = "secret";
        String url = getServerUrl() + "/realms/" + realm + "/protocol/openid-connect/token";

        String form = "grant_type=password" +
                "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                "&username=" + URLEncoder.encode(username, StandardCharsets.UTF_8) +
                "&client_secret=" + URLEncoder.encode(secret, StandardCharsets.UTF_8) +
                "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        log.info(response.body());

        com.fasterxml.jackson.databind.JsonNode node =
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.body());

        return node.get("access_token").asText();
    }

    private String baseUrl() {
        return LOCALHOST_HTTP + localPort;
    }

    private String baseUnsecuredUrl() {
        return LOCALHOST_HTTP + localPort;
    }

    @DynamicPropertySource
    static void registerKeycloakProperties(DynamicPropertyRegistry registry) {
        String authServerUrl = getServerUrl();
        String realm = "iot-platform";

        registry.add("spring.security.oauth2.client.provider.keycloak.issuer-uri",
                () -> authServerUrl + "/realms/" + realm);
        registry.add("spring.security.oauth2.client.provider.keycloak.token-uri",
                () -> authServerUrl + "/realms/" + realm + "/protocol/openid-connect/token");
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> authServerUrl + "/realms/" + realm);
    }

    protected static void runUserSetupScript() throws Exception {
        String scriptAdmin = "src/test/resources/keycloak/create-admin.sh";
        ProcessBuilder pb = new ProcessBuilder("bash", scriptAdmin);
        pb.environment().put("ENV_KEYCLOAK_REALM", "iot-platform");
        pb.environment().put("ENV_KEYCLOAK_URL", getServerUrl());
        pb.environment().put("ENV_KEYCLOAK_ADMIN", "admin");
        pb.environment().put("ENV_KEYCLOAK_ADMIN_PASSWORD", "admin123");
        pb.environment().put("REALM", "test-realm");
        pb.environment().put("ENV_KEYCLOAK_CLIENT", "device-service");
        pb.environment().put("ENV_KEYCLOAK_ADMIN_ROLE", "deviceapp.admin");
        pb.environment().put("ADMIN_USER", "admin");
        pb.environment().put("ADMIN_PASSWORD", "admin123");

        pb.inheritIO();
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("Failed to run Keycloak setup script");
        }
    }

    @SneakyThrows
    @BeforeAll
    public static void init() {
        postgres.start();
        keycloak.start();
        System.setProperty("keycloak.auth-server-url", getAuthServerUrl());
        runUserSetupScript();
    }

    private static String getAuthServerUrl() {
        String realm = "iot-platform";
        return getServerUrl() + "/realms/" + realm;
    }

    private static String getServerUrl() {
        Integer port = keycloak.getFirstMappedPort();
        String host = keycloak.getHost();
        return "http://" + host + ":" + port;
    }

}
