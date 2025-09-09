package ts.andrey.eventcollector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"ts.andrey.iotcommon", "ts.andrey.eventcollector"})
public class EventCollectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventCollectorApplication.class, args);
    }

}
