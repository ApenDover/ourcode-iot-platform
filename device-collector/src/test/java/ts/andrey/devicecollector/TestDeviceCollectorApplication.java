package ts.andrey.devicecollector;

import org.springframework.boot.SpringApplication;

public class TestDeviceCollectorApplication {

	public static void main(String[] args) {
		SpringApplication.from(DeviceCollectorApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
