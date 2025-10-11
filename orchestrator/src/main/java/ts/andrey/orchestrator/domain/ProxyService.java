package ts.andrey.orchestrator.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ts.andrey.device.api.DeviceV1Api;
import ts.andrey.event.api.EventsV1Api;

@Service
@RequiredArgsConstructor
public class ProxyService {

    private final DeviceV1Api deviceV1Api;
    private final EventsV1Api eventsV1Api;

}
