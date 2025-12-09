package ts.andrey.orchestrator.infrastructure.config.transport;

import feign.Client;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import ts.andrey.orchestrator.infrastructure.config.security.OAuth2RequestInterceptor;

@Configuration
public class FeignClientConfiguration {

    @Value("${orchestrator.feign-logger.enabled}")
    private Boolean isLoggingEnabled;

    @Bean
    public Client feignClient() {
        return new LoggingFeignClient(
                BooleanUtils.toBoolean(isLoggingEnabled)
        );
    }

    @Bean
    public OAuth2RequestInterceptor oAuth2RequestInterceptor(OAuth2AuthorizedClientService clientService) {
        return new OAuth2RequestInterceptor(clientService);
    }

}
