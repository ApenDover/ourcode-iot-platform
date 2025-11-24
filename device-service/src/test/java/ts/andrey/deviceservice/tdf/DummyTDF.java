package ts.andrey.deviceservice.tdf;

import lombok.experimental.UtilityClass;

@UtilityClass
public class DummyTDF {

    public static final DummyDevice device = new DummyDevice();
    public static final DummyDeviceEntity deviceEntity = new DummyDeviceEntity();
    public static final DummyDeviceCreateRequest deviceCreateRequest = new DummyDeviceCreateRequest();
    public static final DummyDeviceUpdateRequest deviceUpdateRequest = new DummyDeviceUpdateRequest();
    public static final DummyDeviceVersionUpdateRequest deviceVersionUpdateRequest = new DummyDeviceVersionUpdateRequest();
    public static final DummyDeviceVersionRollbackRequest deviceVersionRollbackRequest = new DummyDeviceVersionRollbackRequest();

}
