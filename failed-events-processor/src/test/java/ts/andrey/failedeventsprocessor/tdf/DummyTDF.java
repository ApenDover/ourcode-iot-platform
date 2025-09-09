package ts.andrey.failedeventsprocessor.tdf;

import lombok.experimental.UtilityClass;

@UtilityClass
public class DummyTDF {

    public static final DummyDevice device = new DummyDevice();
    public static final DummyDeviceError deviceError = new DummyDeviceError();
    public static final DummyDeviceEvent deviceEvent = new DummyDeviceEvent();
    public static final DummyDeviceEventError deviceEventError = new DummyDeviceEventError();
    public static final DummyErrorMeta errorMeta = new DummyErrorMeta();


}
