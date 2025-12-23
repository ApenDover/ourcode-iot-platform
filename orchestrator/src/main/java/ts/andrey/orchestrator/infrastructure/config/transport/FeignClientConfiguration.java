package ts.andrey.orchestrator.infrastructure.config.transport;

import feign.Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import ts.andrey.orchestrator.infrastructure.config.security.OAuth2RequestInterceptor;

import java.util.Set;

@Slf4j
@Configuration
public class FeignClientConfiguration implements ApplicationContextAware {

    public static final String LOAD_BALANCER = "LoadBalancer";
    public static final String FEIGN = "Feign";

    @Value("${orchestrator.log.beautify:false}")
    private boolean isBeautify;

    @Value("${orchestrator.log.masking:false}")
    private boolean isMasking;

    @Value("${orchestrator.log.keys:null}")
    private Set<String> keyForMasking;

    private ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext context) {
        this.context = context;
    }

    @Bean
    public FeignTraceInterceptor feignTraceInterceptor() {
        return new FeignTraceInterceptor();
    }

    @Bean
    @Primary
    public Client loggingFeignClient() {
        final var springClient = getSpringCloudClient();
        return new UniversalLoggingFeignClient(springClient, isBeautify, isMasking, keyForMasking);
    }

    private Client getSpringCloudClient() {
        try {
            final var beanNames = context.getBeanNamesForType(Client.class);

            for (String beanName : beanNames) {
                if (beanName.contains(LOAD_BALANCER) || beanName.contains(FEIGN)) {
                    Client client = context.getBean(beanName, Client.class);
                    if (!(client instanceof UniversalLoggingFeignClient)) {
                        return client;
                    }
                }
            }
        } catch (Exception ignored) {
            log.warn("Не удалось получить Client из Spring Cloud контекста");
        }
        return new Client.Default(null, null);
    }

    @Bean
    public OAuth2RequestInterceptor oAuth2RequestInterceptor(OAuth2AuthorizedClientService clientService) {
        return new OAuth2RequestInterceptor(clientService);
    }

}

