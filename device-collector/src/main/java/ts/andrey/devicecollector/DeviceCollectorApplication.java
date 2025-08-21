package ts.andrey.devicecollector;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ts.andrey.devicecollector.utils.LiquibaseProcessor;

@RequiredArgsConstructor
@SpringBootApplication
public class DeviceCollectorApplication {

    public static void main(String[] args) {
        LiquibaseProcessor.run();
        SpringApplication.run(DeviceCollectorApplication.class, args);
    }

}
