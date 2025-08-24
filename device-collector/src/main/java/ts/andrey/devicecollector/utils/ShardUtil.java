package ts.andrey.devicecollector.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.boot.cfgxml.internal.ConfigLoader;
import org.yaml.snakeyaml.Yaml;
import ts.andrey.devicecollector.configuration.model.MigrationSource;
import ts.andrey.devicecollector.exception.DeviceCollectorException;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@UtilityClass
public class ShardUtil {

    public int getShardNumByString(String deviceId) {
        return Math.abs(deviceId.hashCode()) % 2;
    }

    public String getShardNameByString(String deviceId) {
        return getShardNumByString(deviceId) > 0 ? "shard-1" : "shard-0";
    }

    public List<MigrationSource> loadShardProperties(String appConfigName) {
        try {
            var profile = System.getProperty(
                    "spring.profiles.active",
                    System.getenv("SPRING_PROFILES_ACTIVE")
            );
            if (Objects.isNull(profile)) {
                profile = "default";
            }

            final var fileName = "application" + (profile.equals("default")
                    ? StringUtils.EMPTY
                    : "-" + profile) + ".yml";

            log.info("Loading properties from [{}]", fileName);

            final var yaml = new Yaml();
            final var inputStream = ConfigLoader.class.getClassLoader()
                    .getResourceAsStream(fileName);

            if (inputStream == null) {
                throw new DeviceCollectorException(fileName + " not found in classpath");
            }

            Map<String, Object> obj = yaml.load(inputStream);

            List<Map<String, String>> dataSources = (List<Map<String, String>>)
                    ((Map<String, Object>) obj.get("app"))
                            .get(appConfigName);

            return dataSources.stream()
                    .map(ds -> MigrationSource.builder()
                            .url(ds.get("url"))
                            .username(ds.get("username"))
                            .password(ds.get("password"))
                            .build())
                    .toList();
        } catch (Exception e) {
            throw new DeviceCollectorException("Error loading properties file", e);
        }
    }

}
