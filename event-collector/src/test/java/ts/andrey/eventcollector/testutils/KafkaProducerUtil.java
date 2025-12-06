package ts.andrey.eventcollector.testutils;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Slf4j
public class KafkaProducerUtil {
    private static final String BASE_URL = "http://localhost:";

    private static final Map<String, KafkaProducer<String, SpecificRecord>> producers = new ConcurrentHashMap<>();
    private static final Map<String, Set<String>> existingTopicsCache = new ConcurrentHashMap<>();

    @SneakyThrows
    public static <T extends SpecificRecord> Future<RecordMetadata> sendMessage(
            String bootstrapServers,
            String topic,
            Integer schemaRegistryPort,
            T message) {

        String cacheKey = bootstrapServers + ":" + schemaRegistryPort;

        // Получаем или создаем producer
        KafkaProducer<String, SpecificRecord> producer = producers.computeIfAbsent(
                cacheKey,
                key -> createProducer(bootstrapServers, schemaRegistryPort)
        );

        // Проверяем топик (один раз на комбинацию bootstrapServers + port)
        ensureTopicExists(bootstrapServers, schemaRegistryPort, topic);

        // Отправляем сообщение
        ProducerRecord<String, SpecificRecord> record = createProducerRecord(
                topic,
                message
        );

        return producer.send(record);
    }

    private static KafkaProducer<String, SpecificRecord> createProducer(
            String bootstrapServers,
            Integer schemaRegistryPort) {

        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        producerProps.put("schema.registry.url", BASE_URL + schemaRegistryPort);
        producerProps.put("specific.avro.reader", "true");
        producerProps.put("avro.remove.java.properties", "true");

        // Оптимизации для тестов
        producerProps.put(ProducerConfig.LINGER_MS_CONFIG, 0);
        producerProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        producerProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        producerProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 30000); // 30 секунд

        return new KafkaProducer<>(producerProps);
    }

    @SneakyThrows
    private static synchronized void ensureTopicExists(
            String bootstrapServers,
            Integer schemaRegistryPort,
            String topic) {

        String cacheKey = bootstrapServers + ":" + schemaRegistryPort;
        Set<String> existingTopics = existingTopicsCache.get(cacheKey);

        if (existingTopics == null || !existingTopics.contains(topic)) {
            Properties adminProps = new Properties();
            adminProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            adminProps.put("schema.registry.url", BASE_URL + schemaRegistryPort);

            try (AdminClient adminClient = AdminClient.create(adminProps)) {
                if (existingTopics == null) {
                    existingTopics = adminClient.listTopics().names().get(10, TimeUnit.SECONDS);
                    existingTopicsCache.put(cacheKey, existingTopics);
                }

                if (!existingTopics.contains(topic)) {
                    NewTopic newTopic = new NewTopic(topic, 3, (short) 1); // 3 партиции для тестов
                    adminClient.createTopics(Collections.singleton(newTopic))
                            .all()
                            .get(10, TimeUnit.SECONDS);

                    existingTopics.add(topic);
                    log.info("Топик {} создан (партиций: 3)", topic);
                }
            }
        }
    }

    private static ProducerRecord<String, SpecificRecord> createProducerRecord(
            String topic,
            SpecificRecord message) {

        ProducerRecord<String, SpecificRecord> record = new ProducerRecord<>(
                topic,
                UUID.randomUUID().toString(),
                message
        );

        String traceId = generateRandomHex(32);
        String spanId = generateRandomHex(16);

        log.info("message : {} - traceId : {} - spanId : {}", message, traceId, spanId);

        String traceParent = String.format("00-%s-%s-01", traceId, spanId);
        record.headers().add("traceparent", traceParent.getBytes(StandardCharsets.UTF_8));

        return record;
    }

    private static String generateRandomHex(int length) {
        Random random = new Random();
        byte[] bytes = new byte[length / 2];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public static void cleanup() {
        producers.values().forEach(KafkaProducer::close);
        producers.clear();
        existingTopicsCache.clear();
    }

    @SneakyThrows
    public static <T extends SpecificRecord> List<Future<RecordMetadata>> sendMessages(
            String bootstrapServers,
            String topic,
            Integer schemaRegistryPort,
            List<T> messages) {

        String cacheKey = bootstrapServers + ":" + schemaRegistryPort;
        KafkaProducer<String, SpecificRecord> producer = producers.computeIfAbsent(
                cacheKey, _ -> createProducer(bootstrapServers, schemaRegistryPort)
        );

        ensureTopicExists(bootstrapServers, schemaRegistryPort, topic);

        List<Future<RecordMetadata>> results = new ArrayList<>();
        for (T message : messages) {
            ProducerRecord<String, SpecificRecord> record = createProducerRecord(topic, message);
            results.add(producer.send(record));
        }

        for (Future<RecordMetadata> future : results) {
            future.get(30, TimeUnit.SECONDS);
        }

        return results;
    }

}
