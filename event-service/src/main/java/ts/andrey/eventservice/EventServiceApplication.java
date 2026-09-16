package ts.andrey.eventservice;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class EventServiceApplication {

   public static void main(String[] args) {
       SpringApplication.run(EventServiceApplication.class, args);
   }

    // Заменяет AOP-перехватчик, который раньше регистрировал
    // opentelemetry-spring-boot-starter под @WithSpan.
    @Bean
    public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        return new ObservedAspect(observationRegistry);
    }

}
