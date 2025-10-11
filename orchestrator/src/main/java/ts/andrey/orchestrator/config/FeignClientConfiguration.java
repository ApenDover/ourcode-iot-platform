package ts.andrey.orchestrator.config;

import feign.Client;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

public class FeignClientConfiguration {

    @Bean
    @ConditionalOnProperty(name = "orchestrator.feign-logger.enabled", havingValue = "true")
    public Client feignClient() {
        return new LoggingFeignClient();
    }

}
