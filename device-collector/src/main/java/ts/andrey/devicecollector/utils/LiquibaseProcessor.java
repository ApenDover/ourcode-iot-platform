package ts.andrey.devicecollector.utils;

import com.zaxxer.hikari.HikariDataSource;
import liquibase.integration.spring.SpringLiquibase;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.DefaultResourceLoader;
import ts.andrey.devicecollector.configuration.model.MigrationSource;
import ts.andrey.devicecollector.exception.DeviceCollectorException;

import java.util.Arrays;

@Slf4j
@UtilityClass
public class LiquibaseProcessor {

    public void runLiquibase(MigrationSource migrationSource) {
        final var ds = new HikariDataSource();
        ds.setJdbcUrl(migrationSource.getUrl());
        ds.setUsername(migrationSource.getUsername());
        ds.setPassword(migrationSource.getPassword());
        ds.setDriverClassName("org.postgresql.Driver");

        final var liquibase = new SpringLiquibase();
        liquibase.setDataSource(ds);
        liquibase.setChangeLog("classpath:liquibase/changelog.yml");
        liquibase.setResourceLoader(new DefaultResourceLoader());
        liquibase.setShouldRun(true);
        try {
            liquibase.afterPropertiesSet();
            log.info("Liquibase changelog applied for {}", migrationSource.getUrl());
        } catch (Exception e) {
            throw new DeviceCollectorException("Liquibase failed for shard " + migrationSource.getUrl(), e);
        }
    }

    public void run() {
        ShardUtil.loadShardProperties("dataSources")
                .forEach(LiquibaseProcessor::runLiquibase);
    }

    public void run(String[] args) {
        if (Arrays.asList(args).contains("--spring.profiles.active=test")) {
            return;
        }
        run();
    }

}
