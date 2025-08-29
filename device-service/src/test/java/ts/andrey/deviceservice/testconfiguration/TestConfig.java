package ts.andrey.deviceservice.testconfiguration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;

@Configuration
public class TestConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
