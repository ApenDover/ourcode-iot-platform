package ts.andrey.orchestrator.infrastructure.config.transport;

import feign.Client;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnBean(FeignClientConfiguration.class)
public class FeignClientDebugger {

    @Autowired(required = false)
    private Client feignClient;

    @PostConstruct
    public void debugFeignClient() {
        if (feignClient != null) {
            log.info("=== Feign Client Configuration ===");
            log.info("Client class: {}", feignClient.getClass().getName());
            log.info("Client toString: {}", feignClient.toString());

            // Проверяем обертки
            if (feignClient instanceof UniversalLoggingFeignClient) {
                log.info("Is UniversalLoggingFeignClient");
                // Можно добавить рефлексию чтобы посмотреть внутренний клиент
            }

            if (feignClient instanceof feign.httpclient.ApacheHttpClient) {
                log.info("✅ Using ApacheHttpClient (supports PATCH)");
            } else if (feignClient instanceof feign.Client.Default) {
                log.warn("⚠️ Using Default HttpURLConnection (NO PATCH support!)");
            }
        } else {
            log.warn("No Feign Client bean found!");
        }
    }

}
