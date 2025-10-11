package ts.andrey.orchestrator.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ts.andrey.device.model.DeviceCreateRequest;
import ts.andrey.orchestrator.out.port.DeviceServicePort;

@Service
@RequiredArgsConstructor
public class DeviceProxyService {

    private final DeviceServicePort deviceServicePort;

    public void createDevice(DeviceCreateRequest deviceCreateRequest) {
        deviceServicePort.createDevice(deviceCreateRequest);
    }

}
