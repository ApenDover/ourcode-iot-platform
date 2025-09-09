package ts.andrey.devicecollector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication(scanBasePackages = {"ts.andrey.iotcommon", "ts.andrey.devicecollector"})
@EnableRetry
public class DeviceCollectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeviceCollectorApplication.class, args);
    }

}
