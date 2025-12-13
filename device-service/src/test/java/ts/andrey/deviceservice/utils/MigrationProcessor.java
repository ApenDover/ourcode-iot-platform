package ts.andrey.deviceservice.utils;

import com.zaxxer.hikari.HikariDataSource;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import ts.andrey.deviceservice.config.model.DataSourcesConfig;
import ts.andrey.deviceservice.exception.DeviceServiceException;

@Slf4j
@UtilityClass
public class MigrationProcessor {

    public void runFlyway(DataSourcesConfig.DataSourceInfo migrationSource) {
        final var ds = new HikariDataSource();
        ds.setJdbcUrl(migrationSource.getUrl());
        ds.setUsername(migrationSource.getUsername());
        ds.setPassword(migrationSource.getPassword());
        ds.setDriverClassName("org.postgresql.Driver");
        runFlyway(ds);
    }

    public void runFlyway(HikariDataSource ds) {
        final var flyway = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .load();

        try {
            flyway.migrate();
            log.info("Flyway migrations applied for [{}]", ds.getJdbcUrl());
        } catch (Exception e) {
            throw new DeviceServiceException("Flyway failed for shard " + ds.getJdbcUrl(), e);
        }
    }

}
