package ts.andrey.deviceservice.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import ts.andrey.deviceservice.config.ShardingSphereConfig;

@Slf4j
@UtilityClass
public class ShardUtil {

    public int getShardNumByString(String deviceId, int shardNum) {
        return Math.abs(deviceId.hashCode()) % shardNum;
    }

    public String getShardNameByString(String deviceId, int shardNum) {
        int shard = getShardNumByString(deviceId, shardNum);
        return ShardingSphereConfig.SHARD_NAME + "-" + shard;
    }

}
