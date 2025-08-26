package ts.andrey.devicecollector.tdf;

import com.nashkod.avro.Device;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Assertions;
import ts.andrey.devicecollector.testutils.ShardUtil;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class DummyDevice {

    public Device getDefault() {
        return new Device("deviceId", "deviceType", "meta", Instant.ofEpochMilli(300L));
    }

    public Device getForShardOne() {
        final var device = new Device("idforshard0", "deviceType", "meta", Instant.ofEpochMilli(300L));
        final var shard = ShardUtil.getShardByString(device.getDeviceId());
        Assertions.assertEquals(0, shard);
        return device;
    }

    public Device getForShardOneUpdated() {
        final var device = new Device("idforshard0", "deviceType", "updated", Instant.ofEpochMilli(600L));
        final var shard = ShardUtil.getShardByString(device.getDeviceId());
        Assertions.assertEquals(0, shard);
        return device;
    }

    public Device getForShardTwo() {
        final var device = new Device("idforshard1", "deviceTwoType", "metaTwo", Instant.ofEpochMilli(900L));
        final var shard = ShardUtil.getShardByString(device.getDeviceId());
        Assertions.assertEquals(1, shard);
        return device;
    }

    public Device getInvalid() {
        return new Device(null, "deviceType", "updated", Instant.ofEpochMilli(600L));
    }

    public Device getDefault(int i) {
        return new Device("deviceId-" + i, "deviceType", "meta", Instant.ofEpochMilli(300L));
    }

    public List<Device> getList(int size) {
        final var list = new ArrayList<Device>();
        for (int i = 0; i < size; i++) {
            list.add(getDefault(i));
        }
        return list;
    }

}
