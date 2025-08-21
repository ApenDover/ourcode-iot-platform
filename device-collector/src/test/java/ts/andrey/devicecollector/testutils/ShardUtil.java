package ts.andrey.devicecollector.testutils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ShardUtil {

    public int getShardByString(String deviceId) {
        return Math.abs(deviceId.hashCode()) % 2;
    }

}
