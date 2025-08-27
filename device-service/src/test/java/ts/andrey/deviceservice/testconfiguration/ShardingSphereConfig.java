package ts.andrey.deviceservice.testconfiguration;

import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.driver.api.ShardingSphereDataSourceFactory;
import org.apache.shardingsphere.infra.algorithm.core.config.AlgorithmConfiguration;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.StandardShardingStrategyConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;
import ts.andrey.deviceservice.configuration.model.DataSourcesConfig;
import ts.andrey.deviceservice.exception.DeviceServiceException;
import ts.andrey.deviceservice.utils.MigrationProcessor;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;

@Configuration
@RequiredArgsConstructor
public class ShardingSphereConfig {

    private final DataSourcesConfig dataSourcesConfig;

    @Bean
    public PostgreSQLContainer<?> postgres1() {
        return new PostgreSQLContainer<>("postgres:16");
    }

    @Bean
    public PostgreSQLContainer<?> postgres2() {
        return new PostgreSQLContainer<>("postgres:16");
    }

    @Primary
    @Bean
    public DataSource createShardingDataSource(
            PostgreSQLContainer<?> postgres1, PostgreSQLContainer<?> postgres2
    ) {
        final var deviceTableRule = new ShardingTableRuleConfiguration("t_device", "shard${0..1}.t_device");
        deviceTableRule.setDatabaseShardingStrategy(
                new StandardShardingStrategyConfiguration("device_id", "deviceid_inline")
        );

        final var shardingRule = new ShardingRuleConfiguration();
        shardingRule.getTables().add(deviceTableRule);
        final var props = new Properties();
        props.put("algorithm-expression", "shard$->{Math.abs(device_id.hashCode()) % 2}");
        shardingRule.getShardingAlgorithms().put(
                "deviceid_inline",
                new AlgorithmConfiguration("INLINE", props)
        );

        final var ds0 = new HikariDataSource();
        ds0.setJdbcUrl(postgres1.getJdbcUrl());
        ds0.setUsername(postgres1.getUsername());
        ds0.setPassword(postgres1.getPassword());
        ds0.setDriverClassName("org.postgresql.Driver");

        final var ds1 = new HikariDataSource();
        ds1.setJdbcUrl(postgres2.getJdbcUrl());
        ds1.setUsername(postgres2.getUsername());
        ds1.setPassword(postgres2.getPassword());
        ds1.setDriverClassName("org.postgresql.Driver");

        final var dataSourceMap = new HashMap<String, DataSource>();
        dataSourceMap.put("shard0", ds0);
        dataSourceMap.put("shard1", ds1);

        dataSourcesConfig.getDataSources().forEach(MigrationProcessor::runFlyway);

        try {
            return ShardingSphereDataSourceFactory.createDataSource(
                    dataSourceMap,
                    Collections.singleton(shardingRule),
                    new Properties()
            );
        } catch (SQLException e) {
            throw new DeviceServiceException(e);
        }
    }

}
