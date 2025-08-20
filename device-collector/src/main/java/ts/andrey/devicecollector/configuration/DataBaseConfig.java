package ts.andrey.devicecollector.configuration;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import liquibase.integration.spring.SpringLiquibase;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.List;

@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class DataBaseConfig {

    private List<MigrationSource> dataSources;

    /**
     * Liquibase для каждого шарда — выполняется при старте.
     */
    @PostConstruct
    public void liquibaseForShards() {
        dataSources.forEach(migrationSource -> {
            HikariDataSource ds = new HikariDataSource();
            ds.setJdbcUrl(migrationSource.getUrl());
            ds.setUsername(migrationSource.getUsername());
            ds.setPassword(migrationSource.getPassword());
            ds.setDriverClassName("org.postgresql.Driver");

            SpringLiquibase liquibase = new SpringLiquibase();
            liquibase.setDataSource(ds);
            liquibase.setChangeLog("classpath:liquibase/changelog.yml");
            liquibase.setResourceLoader(new DefaultResourceLoader());
            liquibase.setShouldRun(true);

            try {
                liquibase.afterPropertiesSet();
                log.info("Liquibase changelog applied for {}", migrationSource.getUrl());
            } catch (Exception e) {
                throw new RuntimeException("Liquibase failed for shard " + migrationSource.getUrl(), e);
            }
        });
    }

//    @Bean("liquibaseDataSourceProperties")
//    @ConfigurationProperties("msjc.liquibase.datasource")
//    DataSourceProperties liquibaseDataSourceProperties() {
//        return new DataSourceProperties();
//    }
//
//    @Bean
//    @ConditionalOnBean(name = "liquibaseDataSourceProperties")
//    public SpringLiquibase liquibase(DataSourceProperties liquibaseDataSourceProperties) {
//        SpringLiquibase liquibase = new SpringLiquibase();
//        DataSource dataSource = liquibaseDataSourceProperties.initializeDataSourceBuilder().build();
//        log.info("liquibase dataSource {}", dataSource.getClass());
//        liquibase.setDataSource(dataSource);
//        liquibase.setChangeLog("classpath:liquibase/changelog.yml");
//        liquibase.setResourceLoader(new DefaultResourceLoader());
//        return liquibase;
//    }

}
