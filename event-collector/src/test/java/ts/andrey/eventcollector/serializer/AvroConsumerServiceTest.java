package ts.andrey.eventcollector.serializer;

import com.nashkod.avro.DeviceEvent;
import com.nashkod.avro.EventType;
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import ts.andrey.eventcollector.tdf.DummyTDF;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AvroConsumerServiceTest {

    private static final String TOPIC = "device-events";

    @Test
    void shouldDeserializeDeviceEvent() {
        // GIVEN
        final var schemaRegistryClient = new MockSchemaRegistryClient();

        final var event = DummyTDF.deviceEvent.getDefault();

        final var config = Map.of(
                "schema.registry.url", "mock://test",
                KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true
        );

        final var serializer = new KafkaAvroSerializer(schemaRegistryClient, config);
        final var avroBytes = serializer.serialize(TOPIC, event);

        final var deserializer = new KafkaAvroDeserializer(schemaRegistryClient, config);

        final var consumer = new MockConsumer<String, Object>(OffsetResetStrategy.EARLIEST);
        final var topicPartition = new TopicPartition(TOPIC, 0);
        consumer.assign(List.of(topicPartition));
        consumer.updateBeginningOffsets(Map.of(topicPartition, 0L));

        // WHEN
        consumer.addRecord(new ConsumerRecord<>(TOPIC, 0, 0L, null, deserializer.deserialize(TOPIC, avroBytes)));
        final var records = consumer.poll(java.time.Duration.ofMillis(100));
        final var received = (DeviceEvent) records.iterator().next().value();

        // THEN
        assertEquals("c9a646d3-9c61-4cb7-b8cd-6f3b5e3d0f7a", received.getEventId());
        assertEquals("deviceId", received.getDeviceId());
        assertEquals(125L, received.getTimestamp());
        assertEquals(EventType.TEMPERATURE, received.getType());
        assertEquals("10", received.getPayload());
    }

}
