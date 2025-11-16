package ts.andrey.orchestrator.infrastructure.config;

import feign.Client;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import ts.andrey.orchestrator.infrastructure.config.security.OAuth2RequestInterceptor;

public class FeignClientConfiguration {

    @Bean
    @ConditionalOnProperty(name = "orchestrator.feign-logger.enabled", havingValue = "true")
    public Client feignClient() {
        return new LoggingFeignClient();
    }

    @Bean
    public OAuth2RequestInterceptor oAuth2RequestInterceptor(OAuth2AuthorizedClientService clientService) {
        return new OAuth2RequestInterceptor(clientService);
    }

}
