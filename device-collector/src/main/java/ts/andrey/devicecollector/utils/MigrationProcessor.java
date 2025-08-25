package ts.andrey.devicecollector.utils;

import com.zaxxer.hikari.HikariDataSource;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import ts.andrey.devicecollector.configuration.model.DataSourcesConfig;
import ts.andrey.devicecollector.exception.DeviceCollectorException;

@Slf4j
@UtilityClass
public class MigrationProcessor {

    public void runFlyway(DataSourcesConfig.DataSourceInfo migrationSource) {
        final var ds = new HikariDataSource();
        ds.setJdbcUrl(migrationSource.getUrl());
        ds.setUsername(migrationSource.getUsername());
        ds.setPassword(migrationSource.getPassword());
        ds.setDriverClassName("org.postgresql.Driver");

        try {
            final var flyway = Flyway.configure()
                    .dataSource(ds)
                    .locations("classpath:db/migration")
                    .load();

            flyway.migrate();

            log.info("Flyway migrations applied for [{}]", migrationSource.getUrl());
        } catch (Exception e) {
            throw new DeviceCollectorException("Flyway failed for shard " + migrationSource.getUrl(), e);
        }
    }

}
