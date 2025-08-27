package ts.andrey.deviceservice.tdf;

import lombok.experimental.UtilityClass;
import ts.andrey.dto.DeviceCreateRequest;

@UtilityClass
public class DummyTDF {

    public static final DummyDevice device = new DummyDevice();
    public static final DummyDeviceEntity deviceEntity = new DummyDeviceEntity();
    public static final DummyDeviceCreateRequest deviceCreateRequest = new DummyDeviceCreateRequest();

}
