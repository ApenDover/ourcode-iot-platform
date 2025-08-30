package ts.andrey.deviceservice.tdf;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ts.andrey.dto.DeviceUpdateRequest;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class DummyDeviceUpdateRequest {

    public DeviceUpdateRequest getDefault() {
        final var request = new DeviceUpdateRequest();
        request.setDeviceType("updatedType");
        request.setMeta("updatedMeta");
        return request;
    }

}
