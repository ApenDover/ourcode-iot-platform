package ts.andrey.devicecollector.utils;

import com.zaxxer.hikari.HikariDataSource;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import ts.andrey.devicecollector.config.model.DataSourcesConfig;
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
        runFlyway(ds);
    }

    public void runFlyway(HikariDataSource ds) {
        final var flyway = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .load();

        migrate(flyway);
    }

    private void migrate(Flyway flyway) {
        try {
            flyway.migrate();
            log.info("Flyway migrations applied for [{}]", flyway.getConfiguration().getUrl());
        } catch (Exception e) {
            throw new DeviceCollectorException("Flyway failed for shard " + flyway.getConfiguration().getUrl(), e);
        }
    }

}
