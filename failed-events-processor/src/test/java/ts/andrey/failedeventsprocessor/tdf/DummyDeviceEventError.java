package ts.andrey.failedeventsprocessor.tdf;

import com.nashkod.avro.DeviceEventError;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class DummyDeviceEventError {

    public DeviceEventError getDefault() {
        return new DeviceEventError(
                DummyTDF.deviceEvent.getDefault(),
                DummyTDF.errorMeta.getDefault(),
                2000L
        );
    }

}
