package ts.andrey.devicecollector.testconfiguration;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class LiquibaseConfig {

    @Bean
    public SpringLiquibase liquibase(DataSource dataSource) {
        final var liquibase = new SpringLiquibase();
        liquibase.setChangeLog("classpath:/liquibase/changelog.yml");
        liquibase.setDataSource(dataSource);
        return liquibase;
    }

}
