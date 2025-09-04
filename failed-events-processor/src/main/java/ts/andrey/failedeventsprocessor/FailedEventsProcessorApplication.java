package ts.andrey.failedeventsprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@SpringBootApplication
public class FailedEventsProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(FailedEventsProcessorApplication.class, args);
    }

}
