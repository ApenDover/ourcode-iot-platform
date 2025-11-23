package ts.andrey.orchestrator.infrastructure.out.port.feign;

import org.springframework.cloud.openfeign.FeignClient;
import ts.andrey.event.api.EventsV1Api;
import ts.andrey.orchestrator.infrastructure.config.transport.FeignClientConfiguration;

@FeignClient(name = "event-client",
        url = "${orchestrator.integration.event-service.url}",
        configuration = FeignClientConfiguration.class
)
public interface EventServiceClient extends EventsV1Api {

}
