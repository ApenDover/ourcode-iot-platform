package ts.andrey.failedeventsprocessor.service;

import org.apache.avro.specific.SpecificRecordBase;

public interface ErrorProcessor<T extends SpecificRecordBase> {

    void start(T errorMessage);

}
