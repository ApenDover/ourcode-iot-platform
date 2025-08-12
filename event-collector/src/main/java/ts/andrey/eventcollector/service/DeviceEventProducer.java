package ts.andrey.eventcollector.service;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface DeviceEventProducer {

    CompletableFuture<List<RecordMetadata>> sendEvents(List<? extends SpecificRecordBase> records);

}
