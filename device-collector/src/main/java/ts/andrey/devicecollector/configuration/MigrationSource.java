package ts.andrey.devicecollector.configuration;

import lombok.Data;

@Data
public class MigrationSource {
    private String url;
    private String username;
    private String password;
}
