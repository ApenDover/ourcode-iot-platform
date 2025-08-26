package ts.andrey.devicecollector.configuration.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class DataSourcesConfig {

    private List<DataSourceInfo> dataSources;
    private List<DataSourceInfo> replicaDataSources;

    @Data
    public static class DataSourceInfo {
        private String url;
        private String username;
        private String password;
    }

}
