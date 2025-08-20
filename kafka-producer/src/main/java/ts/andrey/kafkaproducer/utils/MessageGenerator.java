package ts.andrey.kafkaproducer.utils;

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

    private static List<Integer> devicePool = new ArrayList<>();

    public List<DeviceEvent> generate(int messageCount, int deviceCount, boolean persist) {
        List<Integer> ulidPool = persist ? syncDevicePool(deviceCount) : generateDeviceIdPool(deviceCount);

        List<DeviceEvent> events = new ArrayList<>();
        for (int i = 0; i < messageCount; i++) {
            events.add(generateRandomMessage(ulidPool));
        }

        if (!persist) {
            devicePool.clear();
        }
        return events;
    }

    private List<Integer> syncDevicePool(int deviceCount) {
        if (devicePool.size() < deviceCount) {
            devicePool.addAll(generateDeviceIdPool(deviceCount - devicePool.size()));
        } else if (devicePool.size() > deviceCount) {
            devicePool = new ArrayList<>(devicePool.subList(0, deviceCount));
        }
        return devicePool;
    }

    private List<Integer> generateDeviceIdPool(int count) {
        List<Integer> uuids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            uuids.add(i + 1);
        }
        return uuids;
    }

    private DeviceEvent generateRandomMessage(List<Integer> ulidPool) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return new DeviceEvent(
                UUID.randomUUID().toString(),
                generateRandomTimestamp(),
                getRandomElement(List.of(EventType.values())),
                String.format("%.2f", random.nextDouble(RANGE_START, RANGE_END)),
                new Device(
                        (long) getRandomElement(ulidPool),
                        getRandomElement(DEVICE_TYPES),
                        "information",
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
