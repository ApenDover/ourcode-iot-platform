package ts.andrey.devicecollector.utils;

import com.zaxxer.hikari.HikariDataSource;
import liquibase.integration.spring.SpringLiquibase;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.boot.cfgxml.internal.ConfigLoader;
import org.springframework.core.io.DefaultResourceLoader;
import org.yaml.snakeyaml.Yaml;
import ts.andrey.devicecollector.configuration.MigrationSource;

import java.util.List;
import java.util.Map;

@Slf4j
@UtilityClass
public class LiquibaseProcessor {

    public void run() {
        loadProperties().forEach(migrationSource -> {
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
                throw new RuntimeException("Liquibase failed for shard " + migrationSource.getUrl(), e);
            }
        });
    }

    private List<MigrationSource> loadProperties() {
        try {
            final var yaml = new Yaml();
            final var inputStream = ConfigLoader.class.getClassLoader()
                    .getResourceAsStream("application.yml");

            if (inputStream == null) {
                throw new RuntimeException("application.yml not found in classpath");
            }

            Map<String, Object> obj = yaml.load(inputStream);
            List<Map<String, String>> dataSources = (List<Map<String, String>>)
                    ((Map<String, Object>) obj.get("app")).get("dataSources");

            return dataSources.stream()
                    .map(ds -> MigrationSource.builder()
                            .url(ds.get("url"))
                            .username(ds.get("username"))
                            .password(ds.get("password"))
                            .build()).toList();
        } catch (Exception e) {
            throw new RuntimeException("Error loading application.yml", e);
        }
    }

}
