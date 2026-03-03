package ts.andrey.kafkaproducer.utils;

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
    private static final long RANGE_START = -50;
    private static final long RANGE_END = 50;

    private static List<String> devicePool = new ArrayList<>();

    public List<DeviceEvent> generate(long messageCount, long deviceCount, boolean persist) {
        List<String> ulidPool = persist ? syncDevicePool(deviceCount) : generateUlidPool(deviceCount);
        List<DeviceEvent> events = new ArrayList<>();
        for (long i = 0; i < messageCount; i++) {
            events.add(generateRandomMessage(ulidPool));
        }
        if (!persist) {
            devicePool.clear();
        }
        return events;
    }

    private List<String> syncDevicePool(long deviceCount) {
        if (devicePool.size() < deviceCount) {
            devicePool.addAll(generateUlidPool(deviceCount - devicePool.size()));
        } else if (devicePool.size() > deviceCount) {
            devicePool = new ArrayList<>(devicePool.subList(0, (int) deviceCount));
        }
        return devicePool;
    }

    private List<String> generateUlidPool(long count) {
        List<String> ulids = new ArrayList<>((int) count);
        for (long i = 0; i < count; i++) {
            ulids.add(UlidCreator.getUlid().toString());
        }
        return ulids;
    }

    private DeviceEvent generateRandomMessage(List<String> ulidPool) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return new DeviceEvent(
                UUID.randomUUID().toString(),
                generateRandomTimestamp(),
                getRandomElement(List.of(EventType.values())),
                String.format("%.2f", random.nextDouble(RANGE_START, RANGE_END)),
                new Device(
                        getRandomElement(ulidPool),
                        getRandomElement(DEVICE_TYPES),
                        "information",
                        false,
                        generateRandomTimestamp()
                )
        );
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    private Instant generateRandomTimestamp() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        LocalDateTime dateTime = LocalDateTime.of(
                2025,
                random.nextInt(1, 12),
                random.nextInt(1, 29),
                random.nextInt(0, 24),
                random.nextInt(0, 60),
                random.nextInt(0, 60)
        );
        return dateTime.toInstant(ZoneOffset.UTC);
    }

    private <T> T getRandomElement(List<T> list) {
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

}
