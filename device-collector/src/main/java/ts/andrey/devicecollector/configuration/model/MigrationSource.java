package ts.andrey.devicecollector.configuration.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class MigrationSource {

    private String url;
    private String username;
    private String password;

}
