package ts.andrey.orchestrator.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@Slf4j
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "orchestrator.integration.device-service.url=http://localhost:${wiremock.server.port}",
                "orchestrator.integration.event-service.url=http://localhost:${wiremock.server.port}"
        }
)
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureWireMock(port = 0)
public abstract class BaseIntegrationTest {

    protected static final String LOCALHOST_HTTP = "http://localhost:";
    protected static final String KEYCLOAK_REALM = "iot-platform";
    protected static final String KEYCLOAK_ADMIN = "admin";
    protected static final String KEYCLOAK_PASSWORD = "admin123";
    protected static final String KEYCLOAK_CLIENT = "orchestrator";
    protected static final String KEYCLOAK_CLIENT_SECRET = "secret";

    private static final Network NETWORK = Network.newNetwork();

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("keycloak")
            .withUsername("test")
            .withPassword("test")
            .withNetwork(NETWORK)
            .withCreateContainerCmdModifier(cmd -> cmd.withName("postgres"));

    @Container
    static final GenericContainer<?> KEYCLOAK;

    static {
        try {
            KEYCLOAK = new GenericContainer<>("quay.io/keycloak/keycloak:26.3")
                    .withNetwork(NETWORK)
                    .withCreateContainerCmdModifier(cmd -> cmd.withName("keycloak"))
                    .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", KEYCLOAK_ADMIN)
                    .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", KEYCLOAK_PASSWORD)
                    .withEnv("KC_DB", "postgres")
                    .withEnv("KC_DB_URL_HOST", "postgres")
                    .withEnv("KC_DB_URL_PORT", "5432")
                    .withEnv("KC_DB_URL_DATABASE", "keycloak")
                    .withEnv("KC_DB_USERNAME", "test")
                    .withEnv("KC_DB_PASSWORD", "test")
                    .withCopyToContainer(
                            Transferable.of(Files.readAllBytes(
                                    Paths.get("src/test/resources/realm-export.json"))),
                            "/opt/keycloak/data/import/realm-export.json"
                    )
                    .withCommand("start-dev", "--import-realm")
                    .withExposedPorts(8080)
                    .dependsOn(POSTGRES);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Autowired
    protected RestTemplate restTemplate;

    @LocalServerPort
    protected int localPort;

    @Autowired
    protected MeterRegistry meterRegistry;

    protected String getUrl(String path) {
        return path.startsWith("/") ? baseUrl() + path : baseUrl() + "/" + path;
    }

    protected String getUnsecuredUrl(String path) {
        return path.startsWith("/") ? baseUnsecuredUrl() + path : baseUnsecuredUrl() + "/" + path;
    }

    private String baseUrl() {
        return LOCALHOST_HTTP + localPort;
    }

    private String baseUnsecuredUrl() {
        return LOCALHOST_HTTP + localPort;
    }


    protected ResponseEntity<String> sendGet(String path, String jwtToken) {
        HttpHeaders headers = createJsonHeaders(jwtToken);
        return restTemplate.exchange(getUrl(path), HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    protected ResponseEntity<String> sendGetUnsecured(String path) {
        return restTemplate.getForEntity(getUnsecuredUrl(path), String.class);
    }

    protected <Req, Res> ResponseEntity<Res> sendPost(String path, Req body, Class<Res> responseClass) {
        final var jwtToken = getAccessToken("admin", "admin123");
        HttpHeaders headers = createJsonHeaders(jwtToken);
        return restTemplate.postForEntity(getUrl(path), new HttpEntity<>(body, headers), responseClass);
    }

    protected <Res> Res sendPostOk(String path, Object body, Class<Res> responseClass) {
        ResponseEntity<Res> response = sendPost(path, body, responseClass);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertInstanceOf(responseClass, response.getBody());
        return response.getBody();
    }

    private HttpHeaders createJsonHeaders(String jwtToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (jwtToken != null) {
            headers.setBearerAuth(jwtToken);
        }
        return headers;
    }


    @SneakyThrows
    protected static String getAccessToken(String username, String password) {
        final var url = getServerUrl() + "/realms/" + KEYCLOAK_REALM + "/protocol/openid-connect/token";

        final var form = "grant_type=password"
                + "&client_id=" + URLEncoder.encode(KEYCLOAK_CLIENT, StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(KEYCLOAK_CLIENT_SECRET, StandardCharsets.UTF_8)
                + "&username=" + URLEncoder.encode(username, StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);

        final var request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        final var client = HttpClient.newHttpClient();
        final var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        log.info(response.body());
        return new ObjectMapper().readTree(response.body()).get("access_token").asText();
    }

    @SneakyThrows
    public <Res> ResponseEntity<Res> sendRequest(String url, HttpMethod method,
                                                 Object body, Class<Res> responseType) {
        final var jwtToken = getAccessToken("admin", "admin123");
        final var headers = new HttpHeaders();
        if (jwtToken != null) {
            headers.setBearerAuth(jwtToken);
        }
        headers.setContentType(MediaType.APPLICATION_JSON);
        final var entity = new HttpEntity<>(body, headers);
        try {
            return restTemplate.exchange(getUrl(url), method, entity, responseType);
        } catch (HttpServerErrorException exception) {
            Res errorBody = exception.getResponseBodyAs(responseType);
            return ResponseEntity.status(exception.getStatusCode())
                    .headers(exception.getResponseHeaders())
                    .body(errorBody);
        }
    }

    @DynamicPropertySource
    static void registerKeycloakProperties(DynamicPropertyRegistry registry) {
        final var authServerUrl = getServerUrl();
        registry.add("spring.security.oauth2.client.provider.keycloak.issuer-uri",
                () -> authServerUrl + "/realms/" + KEYCLOAK_REALM);
        registry.add("spring.security.oauth2.client.provider.keycloak.token-uri",
                () -> authServerUrl + "/realms/" + KEYCLOAK_REALM + "/protocol/openid-connect/token");
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
                () -> authServerUrl + "/realms/" + KEYCLOAK_REALM);
    }

    protected static void runUserSetupScript() throws Exception {
        final var pb = new ProcessBuilder("bash", "src/test/resources/keycloak/create-admin-orchestrator.sh");
        final var env = pb.environment();
        env.put("ENV_KEYCLOAK_REALM", KEYCLOAK_REALM);
        env.put("ENV_KEYCLOAK_URL", getServerUrl());
        env.put("ENV_KEYCLOAK_ADMIN", KEYCLOAK_ADMIN);
        env.put("ENV_KEYCLOAK_ADMIN_PASSWORD", KEYCLOAK_PASSWORD);
        env.put("ENV_KEYCLOAK_ORCHESTRATOR_CLIENT", KEYCLOAK_CLIENT);
        env.put("REALM", "test-realm");
        env.put("ENV_KEYCLOAK_ORCHESTRATOR_ADMIN_ROLE", "deviceapp.admin");
        env.put("ADMIN_USER", KEYCLOAK_ADMIN);
        env.put("ADMIN_PASSWORD", KEYCLOAK_PASSWORD);

        pb.inheritIO();
        final var process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("Failed to run Keycloak setup script");
        }
    }

    protected static void runRolesSetupScript() throws Exception {
        final var pb = new ProcessBuilder("bash", "src/test/resources/keycloak/create-user-device.sh");
        final var env = pb.environment();
        env.put("ENV_KEYCLOAK_REALM", KEYCLOAK_REALM);
        env.put("ENV_KEYCLOAK_URL", getServerUrl());
        env.put("ENV_KEYCLOAK_ADMIN", KEYCLOAK_ADMIN);
        env.put("ENV_KEYCLOAK_ADMIN_PASSWORD", KEYCLOAK_PASSWORD);
        env.put("ENV_KEYCLOAK_ORCHESTRATOR_CLIENT", KEYCLOAK_CLIENT);
        env.put("REALM", "test-realm");
        env.put("ENV_KEYCLOAK_ORCHESTRATOR_ADMIN_ROLE", "deviceapp.admin");
        env.put("ADMIN_USER", KEYCLOAK_ADMIN);
        env.put("ADMIN_PASSWORD", KEYCLOAK_PASSWORD);

        pb.inheritIO();
        final var process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("Failed to run Keycloak setup script");
        }
    }

    @BeforeAll
    static void init() throws Exception {
        POSTGRES.start();
        KEYCLOAK.start();
        System.setProperty("keycloak.auth-server-url", getAuthServerUrl());
        runUserSetupScript();
        runRolesSetupScript();
    }

    private static String getAuthServerUrl() {
        return getServerUrl() + "/realms/" + KEYCLOAK_REALM;
    }

    private static String getServerUrl() {
        final var port = KEYCLOAK.getFirstMappedPort();
        return "http://" + KEYCLOAK.getHost() + ":" + port;
    }

}
