package ts.andrey.orchestrator.out.port;

import org.springframework.cloud.openfeign.FeignClient;
import ts.andrey.device.api.DeviceV1Api;
import ts.andrey.orchestrator.config.FeignClientConfiguration;

@FeignClient(name = "event-client",
        url = "${orchestrator.integration.event-service.url}",
        configuration = FeignClientConfiguration.class
)
public interface EventServicePort extends DeviceV1Api {

}
