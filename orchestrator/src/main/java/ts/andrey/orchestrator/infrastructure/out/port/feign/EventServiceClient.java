package ts.andrey.orchestrator.infrastructure.out.port.feign;

import org.springframework.cloud.openfeign.FeignClient;
import ts.andrey.event.api.EventsV1Api;

@FeignClient(name = "event-client",
        url = "${orchestrator.integration.event-service.url}"
)
public interface EventServiceClient extends EventsV1Api {

}
