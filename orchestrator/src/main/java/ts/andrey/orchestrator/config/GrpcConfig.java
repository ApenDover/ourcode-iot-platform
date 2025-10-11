package ts.andrey.orchestrator.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcConfig {

    @Value("${orchestrator.integration.router-manager.host:localhost}")
    private String routerManagerHost;

    @Value("${orchestrator.integration.router-manager.port:9090}")
    private int routerManagerPort;

    @Bean
    public ManagedChannel routerManagerChannel() {
        return ManagedChannelBuilder
                .forAddress(routerManagerHost, routerManagerPort)
                .usePlaintext()
                .build();
    }

}
