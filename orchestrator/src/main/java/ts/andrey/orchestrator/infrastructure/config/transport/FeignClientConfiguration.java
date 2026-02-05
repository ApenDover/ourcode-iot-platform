package ts.andrey.orchestrator.infrastructure.config.transport;

import feign.Client;
import feign.Request;
import feign.httpclient.ApacheHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.support.FeignHttpClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import ts.andrey.orchestrator.infrastructure.config.security.OAuth2RequestInterceptor;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FeignClientConfiguration {

    @Value("${orchestrator.log.beautify:false}")
    private boolean isBeautify;

    @Value("${orchestrator.log.masking:false}")
    private boolean isMasking;

    @Value("${orchestrator.log.keys:null}")
    private Set<String> keyForMasking;

    @Value("${spring.cloud.openfeign.httpclient.read-timeout-ms:3000}")
    private int readTimeoutMs;

    private final FeignHttpClientProperties feignHttpClientProperties;

    @Bean
    public FeignTraceInterceptor feignTraceInterceptor() {
        return new FeignTraceInterceptor();
    }

    @Bean
    @Primary
    public Client loggingFeignClient() {
        final var connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(
                feignHttpClientProperties.getMaxConnections()
        );
        connectionManager.setDefaultMaxPerRoute(
                feignHttpClientProperties.getMaxConnectionsPerRoute()
        );

        final var requestConfig = RequestConfig.custom()
                .setConnectTimeout(
                        feignHttpClientProperties.getConnectionTimeout()
                )
                .setSocketTimeout(
                        readTimeoutMs
                )
                .build();

        final var httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(connectionManager)
                .build();
        final var client = new ApacheHttpClient(httpClient);
        return new UniversalLoggingFeignClient(client, isBeautify, isMasking, keyForMasking);
    }

    @Bean
    public Request.Options feignRequestOptions() {
        return new Request.Options(
                feignHttpClientProperties.getConnectionTimeout(),
                TimeUnit.MILLISECONDS,
                readTimeoutMs,
                TimeUnit.MILLISECONDS,
                true
        );
    }

    @Bean
    public OAuth2RequestInterceptor oAuth2RequestInterceptor(OAuth2AuthorizedClientService clientService) {
        return new OAuth2RequestInterceptor(clientService);
    }

}
