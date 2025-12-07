package ts.andrey.iotcommon.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GlobalKafkaMetrics {

    private final MeterRegistry meterRegistry;

    public void incrementSuccess(String topic) {
        Counter counter = Counter.builder("iot_kafka_messages_sent")
                .description("Successfully sent Kafka messages")
                .tag("topic", topic)
                .register(meterRegistry);
        counter.increment();
    }

    public void incrementDlt(String topic) {
        Counter counter = Counter.builder("iot_kafka_messages_dlt_success")
                .description("Messages sent to Dead Letter Topic")
                .tag("topic", topic)
                .register(meterRegistry);
        counter.increment();
    }

    public void incrementErrorDlt() {
        Counter counter = Counter.builder("iot_kafka_messages_dlt_fail")
                .description("Messages do not sent to Dead Letter Topic")
                .register(meterRegistry);
        counter.increment();
    }

}
