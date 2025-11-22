package ts.andrey.deviceservice.tdf;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ts.andrey.dto.DeviceStatus;
import ts.andrey.dto.DeviceVersionRollbackRequest;
import ts.andrey.dto.DeviceVersionUpdateRequest;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class DummyDeviceVersionRollbackRequest {

    public DeviceVersionRollbackRequest getDefault() {
        final var request = new DeviceVersionRollbackRequest();
        request.setRollbackVersion("0.0.0");
        request.setStatus(DeviceStatus.READY);
        request.setEtag(1L);
        return request;
    }

}
