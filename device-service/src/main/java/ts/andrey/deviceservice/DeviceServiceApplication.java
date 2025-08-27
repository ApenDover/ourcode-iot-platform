package ts.andrey.deviceservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.UUID;

@SpringBootApplication
public class DeviceServiceApplication {

    public static void main(String[] args) {
        System.out.println(UUID.randomUUID().toString());
//        SpringApplication.run(DeviceServiceApplication.class, args);
    }

}
