package ts.andrey.failedeventsprocessor.utils;

import lombok.experimental.UtilityClass;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;

import java.io.ByteArrayOutputStream;

@UtilityClass
public class JsonAvroConverter {

    public byte[] toJsonAvro(SpecificRecordBase avroObject) {
        try {
            final var writer = new SpecificDatumWriter<SpecificRecordBase>(avroObject.getSchema());
            final var out = new ByteArrayOutputStream();
            final var encoder = EncoderFactory.get().jsonEncoder(avroObject.getSchema(), out);
            writer.write(avroObject, encoder);
            encoder.flush();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
