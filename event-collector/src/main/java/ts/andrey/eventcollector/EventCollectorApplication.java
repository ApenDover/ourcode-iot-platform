package ts.andrey.eventcollector;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication(scanBasePackages = {"ts.andrey.iotcommon", "ts.andrey.eventcollector"})
@EnableAspectJAutoProxy
public class EventCollectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventCollectorApplication.class, args);
    }

    // Раньше @WithSpan работал через AOP-инфраструктуру
    // opentelemetry-spring-boot-starter — здесь не было ни AspectJ на
    // classpath, ни @EnableAspectJAutoProxy. Добавили и то, и то (см.
    // build.gradle), иначе @Observed не будет перехватываться.
    @Bean
    public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }

}
