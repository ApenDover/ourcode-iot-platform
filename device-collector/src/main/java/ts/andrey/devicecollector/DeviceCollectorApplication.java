package ts.andrey.devicecollector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ts.andrey.devicecollector.utils.LiquibaseProcessor;

import java.util.Arrays;


@SpringBootApplication
public class DeviceCollectorApplication {

    public static void main(String[] args) {
        if (!Arrays.asList(args).contains("--spring.profiles.active=test")) {
            LiquibaseProcessor.run();
        }
        SpringApplication.run(DeviceCollectorApplication.class, args);
    }

}
