package ts.andrey.orchestrator.out.port;

import org.springframework.cloud.openfeign.FeignClient;
import ts.andrey.device.api.DeviceV1Api;
import ts.andrey.orchestrator.config.FeignClientConfiguration;

@FeignClient(name = "device-client",
        url = "${orchestrator.integration.device-service.url}",
        configuration = FeignClientConfiguration.class
)
public interface DeviceServicePort extends DeviceV1Api {

}
