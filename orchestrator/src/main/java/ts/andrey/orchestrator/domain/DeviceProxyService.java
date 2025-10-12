package ts.andrey.orchestrator.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ts.andrey.device.model.DeviceCreateRequest;
import ts.andrey.orchestrator.infrastructure.feign.DeviceServiceClient;

@Service
@RequiredArgsConstructor
public class DeviceProxyService {

    private final DeviceServiceClient deviceServiceClient;

    public void createDevice(DeviceCreateRequest deviceCreateRequest) {
        deviceServiceClient.createDevice(deviceCreateRequest);
    }

}
