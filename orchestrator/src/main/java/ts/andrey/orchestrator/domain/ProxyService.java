package ts.andrey.orchestrator.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ts.andrey.orchestrator.out.port.DeviceServicePort;
import ts.andrey.orchestrator.out.port.EventServicePort;

@Service
@RequiredArgsConstructor
public class ProxyService {

    private final DeviceServicePort deviceServicePort;
    private final EventServicePort eventServicePort;

}
