package ts.andrey.eventcollector.utils;

import com.github.f4b6a3.ulid.UlidCreator;
import com.nashkod.avro.Device;
import com.nashkod.avro.DeviceEvent;
import com.nashkod.avro.EventType;
import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public class MessageGenerator {

    private static final List<String> DEVICE_TYPES = List.of("sensor", "actuator", "controller", "gateway");
    private static final int RANGE_START = -50;
    private static final int RANGE_END = 50;

    public List<DeviceEvent> generate(Integer messageCount, Integer deviceCount) {
        final var events = new ArrayList<DeviceEvent>();
        final var ulidPool = generateUlidPool(deviceCount);

        for (int i = 0; i < messageCount; i++) {
            events.add(generateRandomMessage(ulidPool));
        }

        return events;
    }

    private List<String> generateUlidPool(int poolSize) {
        final var ulids = new ArrayList<String>();
        for (int i = 0; i < poolSize; i++) {
            ulids.add(UlidCreator.getUlid().toString());
        }
        return ulids;
    }

    private DeviceEvent generateRandomMessage(List<String> ulidPool) {
        final var random = ThreadLocalRandom.current();
        return new DeviceEvent(
                UUID.randomUUID().toString(), // eventId
                generateRandomTimestamp(),    // timestamp
                (EventType) getRandomElement(List.of(EventType.values())),
                String.format("%.2f", random.nextDouble(RANGE_START, RANGE_END)),
                new Device(
                        (String) getRandomElement(ulidPool),
                        (String) getRandomElement(DEVICE_TYPES),
                        "information",
                        generateRandomTimestamp()
                )
        );
    }

    private Instant generateRandomTimestamp() {
        final var random = ThreadLocalRandom.current();
        final var year = random.nextInt(1900, 2101);
        final var month = random.nextInt(1, 13);
        final var day = random.nextInt(1, 29);
        final var hour = random.nextInt(0, 24);
        final var minute = random.nextInt(0, 60);
        final var second = random.nextInt(0, 60);

        return Instant.ofEpochSecond(
                LocalDateTime.of(year, month, day, hour, minute, second)
                        .toEpochSecond(ZoneOffset.UTC)
        );
    }

    private <T> Object getRandomElement(List<T> list) {
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

}
