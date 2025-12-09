package ts.andrey.orchestrator.infrastructure.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "orchestrator.filter")
public class RequestFilterProperties {

    private boolean enabled = true;
    private boolean logBody = true;
    private boolean logHeaders = true;
    private int maxBodySize = 1024;
    private boolean asyncMetrics = true;
    private List<String> excludedPaths = new ArrayList<>();
    private List<String> sensitivePaths = new ArrayList<>();

    public RequestFilterProperties() {
        excludedPaths.add("/actuator/**");
        excludedPaths.add("/health");
        excludedPaths.add("/metrics");
        excludedPaths.add("/prometheus");
    }

}
