package ts.andrey.devicecollector.configuration;

import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.driver.api.ShardingSphereDataSourceFactory;
import org.apache.shardingsphere.driver.yaml.YamlJDBCConfiguration;
import org.apache.shardingsphere.infra.algorithm.core.config.AlgorithmConfiguration;
import org.apache.shardingsphere.readwritesplitting.config.ReadwriteSplittingRuleConfiguration;
import org.apache.shardingsphere.readwritesplitting.config.rule.ReadwriteSplittingDataSourceGroupRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.StandardShardingStrategyConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import ts.andrey.devicecollector.exception.DeviceCollectorException;
import ts.andrey.devicecollector.utils.LiquibaseProcessor;
import ts.andrey.devicecollector.utils.ShardUtil;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Configuration
@RequiredArgsConstructor
public class ShardingSphereConfig {

    @Primary
    @Bean
    public DataSource createShardingDataSource() {
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

        final var masterSource = ShardUtil.loadShardProperties("dataSources");
        final var replicaSource = ShardUtil.loadShardProperties("replicaDataSources");

        final var dataSourceMap = new HashMap<String, DataSource>();

        for (int i = 0; i < masterSource.size(); i++) {
            final var source = masterSource.get(i);
            final var ds = new HikariDataSource();
            ds.setJdbcUrl(source.getUrl());
            ds.setUsername(source.getUsername());
            ds.setPassword(source.getPassword());
            ds.setDriverClassName("org.postgresql.Driver");
            dataSourceMap.put("shard" + i, ds);
        }

        for (int i = 0; i < replicaSource.size(); i++) {
            final var source = masterSource.get(i);
            final var ds = new HikariDataSource();
            ds.setJdbcUrl(source.getUrl());
            ds.setUsername(source.getUsername());
            ds.setPassword(source.getPassword());
            ds.setDriverClassName("org.postgresql.Driver");
            dataSourceMap.put("shard" + i + "_replica", ds);
        }

        // --- READWRITE SPLITTING ---
        YamlJDBCConfiguration conf = new YamlJDBCConfiguration();
        conf.setDatabaseName("iot_platform");

        ReadwriteSplittingDataSourceGroupRuleConfiguration shard0Group =
                new ReadwriteSplittingDataSourceGroupRuleConfiguration(
                        "shard0",
                        "shard0",
                        Collections.singletonList("shard0_replica"),
                        "round_robin"
                );

        ReadwriteSplittingDataSourceGroupRuleConfiguration shard1Group =
                new ReadwriteSplittingDataSourceGroupRuleConfiguration(
                        "shard1",
                        "shard1",
                        List.of("shard1_replica"),
                        "round_robin"
                );


        final var loadBalancers = Map.<String, AlgorithmConfiguration>of(
                "round_robin", new AlgorithmConfiguration("ROUND_ROBIN", new Properties())
        );

        masterSource.forEach(LiquibaseProcessor::runLiquibase);

        final var readwriteRule = new ReadwriteSplittingRuleConfiguration(List.of(shard0Group, shard1Group), loadBalancers);

        try {
            return ShardingSphereDataSourceFactory.createDataSource(
                    dataSourceMap,
                    new HashSet<>(Arrays.asList(shardingRule, readwriteRule)), // <--- тут оба
                    new Properties()
            );
        } catch (SQLException e) {
            throw new DeviceCollectorException(e);
        }
    }

}
