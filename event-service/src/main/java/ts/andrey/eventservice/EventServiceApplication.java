package ts.andrey.eventservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class EventServiceApplication {

   public static void main(String[] args) {
       SpringApplication.run(EventServiceApplication.class, args);
   }

}
