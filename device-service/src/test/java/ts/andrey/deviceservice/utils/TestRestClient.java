package ts.andrey.deviceservice.utils;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@Component
@RequiredArgsConstructor
public class TestRestClient {

    private final RestTemplate restTemplate;
    private static final String LOCALHOST_HTTP = "http://localhost:";


}
