package ts.andrey.orchestrator.infrastructure.feign;

import org.springframework.cloud.openfeign.FeignClient;
import ts.andrey.device.api.DeviceV1Api;
import ts.andrey.orchestrator.infrastructure.config.FeignClientConfiguration;

@FeignClient(name = "device-client",
        url = "${orchestrator.integration.device-service.url}",
        configuration = FeignClientConfiguration.class
)
public interface DeviceServiceClient extends DeviceV1Api {

}
