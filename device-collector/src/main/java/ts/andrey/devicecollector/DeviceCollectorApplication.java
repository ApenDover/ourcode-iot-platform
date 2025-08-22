package ts.andrey.devicecollector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ts.andrey.devicecollector.utils.LiquibaseProcessor;


@SpringBootApplication
public class DeviceCollectorApplication {

    public static void main(String[] args) {
        LiquibaseProcessor.run(args);
        SpringApplication.run(DeviceCollectorApplication.class, args);
    }

}
