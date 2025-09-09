package ts.andrey.failedeventsprocessor.tdf;

import com.nashkod.avro.ErrorMeta;
import com.nashkod.avro.ErrorSource;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class DummyErrorMeta {

    public ErrorMeta getDefault() {
        return new ErrorMeta(ErrorSource.DEVICE_COLLECTOR, "reason", "stackTrace");
    }

}
