package ts.andrey.deviceservice.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ts.andrey.deviceservice.service.DeviceService;

@Configuration
public class AppConfiguration {

    @Value("${app.redis.enabled}")
    private boolean redisEnabled;

    @Bean
    public DeviceService deviceService(DeviceService deviceCacheServiceImpl, DeviceService deviceDataServiceImpl) {
        return redisEnabled ? deviceCacheServiceImpl : deviceDataServiceImpl;
    }

}
