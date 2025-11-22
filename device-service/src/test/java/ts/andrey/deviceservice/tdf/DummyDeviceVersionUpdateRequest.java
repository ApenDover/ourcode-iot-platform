package ts.andrey.deviceservice.tdf;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ts.andrey.dto.DeviceUpdateRequest;
import ts.andrey.dto.DeviceVersionUpdateRequest;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class DummyDeviceVersionUpdateRequest {

    public DeviceVersionUpdateRequest getDefault() {
        final var request = new DeviceVersionUpdateRequest();
        request.setEtag(0L);
        request.setTargetVersion("2.1.1");
        return request;
    }

}
