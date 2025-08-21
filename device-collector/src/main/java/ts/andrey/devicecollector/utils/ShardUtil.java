package ts.andrey.devicecollector.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ShardUtil {

    public int getShardNumByString(String deviceId) {
        return Math.abs(deviceId.hashCode()) % 2;
    }

    public String getShardNameByString(String deviceId) {
        return getShardNumByString(deviceId) > 0 ? "shard-1" : "shard-0";
    }

}
