package ts.andrey.orchestrator.infrastructure.out.port.feign;

import org.springframework.cloud.openfeign.FeignClient;
import ts.andrey.device.api.DeviceV1Api;
import ts.andrey.orchestrator.infrastructure.config.transport.FeignClientConfiguration;

@FeignClient(name = "device-client",
        url = "${orchestrator.integration.device-service.url}",
        configuration = FeignClientConfiguration.class
)
public interface DeviceServiceClient extends DeviceV1Api {

}
