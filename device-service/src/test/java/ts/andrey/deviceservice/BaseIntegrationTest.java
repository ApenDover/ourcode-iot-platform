package ts.andrey.deviceservice;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import ts.andrey.deviceservice.data.repository.DeviceRepository;

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

    private static final String LOCALHOST_HTTP = "http://localhost:";
//    private static final String LOCALHOST_HTTPS = "https://localhost:";

    @Autowired
    public TestRestTemplate restTemplate;

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

    public ResponseEntity<String> sendGet(String path) {
        return restTemplate.getForEntity(getUrl(path), String.class);
    }

    public ResponseEntity<Object> sendPost(String path, Object object) {
        return restTemplate.postForEntity(getUrl(path), object, Object.class);
    }

    public <Req, Res> ResponseEntity<Res> sendPost(String path, Req request, Class<Res> responseClass) {
        return restTemplate.postForEntity(getUrl(path), request, responseClass);
    }

    public <Req, Res> Res sendPostOk(String url, Req request, Class<Res> responseClass) {
        return sendPostOk(url, new HttpEntity<>(request), responseClass);
    }

    public <Res> Res sendPostOk(String url, HttpEntity<?> httpEntity, Class<Res> responseClass) {
        final var actual = sendPost(url, httpEntity, responseClass);
        assertEquals(HttpStatus.OK, actual.getStatusCode());
        final var body = actual.getBody();
        assertInstanceOf(responseClass, body);
        return body;
    }

    public <Res> ResponseEntity<Res> sendRequest(String url, HttpMethod method,
                                                 HttpEntity<?> body, Class<Res> responseType) {
        return restTemplate.exchange(getUrl(url), method, body, responseType);
    }

    public ResponseEntity<String> sendGetHttpUnsecured(String path) {
        String httpUnsecuredUrl = getUnsecuredUrl(path);
        return restTemplate.getForEntity(httpUnsecuredUrl, String.class);
    }

    private String baseUrl() {
        return LOCALHOST_HTTP + localPort;
    }

    private String baseUnsecuredUrl() {
        return LOCALHOST_HTTP + localPort;
    }

}
