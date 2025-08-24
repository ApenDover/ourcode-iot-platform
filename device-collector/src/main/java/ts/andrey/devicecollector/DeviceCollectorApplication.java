package ts.andrey.devicecollector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import ts.andrey.devicecollector.utils.LiquibaseProcessor;

@SpringBootApplication
@EnableRetry
public class DeviceCollectorApplication {

    public static void main(String[] args) {
        LiquibaseProcessor.run(args);
        SpringApplication.run(DeviceCollectorApplication.class, args);
    }

}
