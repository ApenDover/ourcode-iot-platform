package ts.andrey.eventcollector.service;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface KafkaProducer {

    CompletableFuture<List<RecordMetadata>> send(List<? extends SpecificRecordBase> records);

}
