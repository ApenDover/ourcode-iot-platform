package ts.andrey.orchestrator.infrastructure.out.port.feign;

import org.springframework.cloud.openfeign.FeignClient;
import ts.andrey.device.api.DeviceV1Api;

@FeignClient(name = "device-client",
        url = "${orchestrator.integration.device-service.url}"
)
public interface DeviceServiceClient extends DeviceV1Api {

}
