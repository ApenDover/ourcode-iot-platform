package ts.andrey.orchestrator.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ts.andrey.orchestrator.infrastructure.feign.DeviceServiceClient;

@Service
@RequiredArgsConstructor
public class DeviceSagaUseCase {

    private final DeviceServiceClient deviceServiceClient;

    public void update() {
        // сначала доработать deviceService с версионированием
//        deviceServiceClient.
    }

}
