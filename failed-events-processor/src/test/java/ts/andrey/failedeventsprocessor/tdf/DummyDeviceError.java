package ts.andrey.failedeventsprocessor.tdf;

import com.nashkod.avro.DeviceError;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class DummyDeviceError {

    public DeviceError getDefault() {
        return new DeviceError(
                DummyTDF.device.getDefault(),
                DummyTDF.errorMeta.getDefault(),
                1000L
        );
    }

}
