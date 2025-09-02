package ts.andrey.deviceservice.tdf;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ts.andrey.dto.DeviceCreateRequest;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class DummyDeviceCreateRequest {

    public DeviceCreateRequest getDefault() {
        final var request = new DeviceCreateRequest();
        request.setDeviceType("deviceType");
        request.setMeta("meta");
        return request;
    }

}
