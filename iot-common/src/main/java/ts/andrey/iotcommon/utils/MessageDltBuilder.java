package ts.andrey.iotcommon.utils;


import com.nashkod.avro.Device;
import com.nashkod.avro.DeviceError;
import com.nashkod.avro.DeviceEvent;
import com.nashkod.avro.DeviceEventError;
import com.nashkod.avro.ErrorMeta;
import com.nashkod.avro.ErrorSource;
import lombok.experimental.UtilityClass;
import org.apache.avro.specific.SpecificRecordBase;
import ts.andrey.iotcommon.exception.IotException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;

@UtilityClass
public class MessageDltBuilder {

    public SpecificRecordBase getMessage(SpecificRecordBase avroMessage, Throwable ex) {
        final var sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        final var meta = new ErrorMeta(ErrorSource.EVENT_COLLECTOR, ex.getMessage(), sw.toString());
        if (avroMessage instanceof DeviceEventError || avroMessage instanceof DeviceError) {
            return avroMessage;
        }
        if (avroMessage instanceof DeviceEvent deviceEvent) {
            return new DeviceEventError(deviceEvent, meta, Instant.now().toEpochMilli());
        }
        if (avroMessage instanceof Device device) {
            return new DeviceError(device, meta, Instant.now().toEpochMilli());
        }
        throw new IotException("незнакомый тип AVRO");
    }

}
