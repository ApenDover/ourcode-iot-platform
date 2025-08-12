package ts.andrey.eventcollector.tdf;

import lombok.experimental.UtilityClass;

@UtilityClass
public class DummyTDF {

    public static final DummyDeviceEvent deviceEvent = new DummyDeviceEvent();
    public static final DummyDevice device = new DummyDevice();
    public static final DummyDeviceEventEntity deviceEventEntity = new DummyDeviceEventEntity();

}
