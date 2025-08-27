package ts.andrey.deviceservice.configuration;

import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.driver.api.ShardingSphereDataSourceFactory;
import org.apache.shardingsphere.infra.algorithm.core.config.AlgorithmConfiguration;
import org.apache.shardingsphere.readwritesplitting.config.ReadwriteSplittingRuleConfiguration;
import org.apache.shardingsphere.readwritesplitting.config.rule.ReadwriteSplittingDataSourceGroupRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.StandardShardingStrategyConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import ts.andrey.deviceservice.configuration.model.DataSourcesConfig;
import ts.andrey.deviceservice.exception.DeviceServiceException;
import ts.andrey.deviceservice.utils.MigrationProcessor;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.stream.IntStream;

@Configuration
@RequiredArgsConstructor
@Profile("!test")
public class ShardingSphereConfig {

    private static final String ALGORITHM_NAME = "deviceid_inline";

    public static final String SHARD_NAME = "shard";

    private static final String REPLICA_POSTFIX = "_replica";

    private static final String POSTGRES_DRIVER = "org.postgresql.Driver";

    private static final String LOAD_BALANCERS_TYPE = "round_robin";

    @Value("${app.shardingSphere.logicTable}")
    private String logicTable;

    @Value("${app.shardingSphere.shardingColumn}")
    private String shardingColumn;

    @Value("${app.shardingSphere.algorithmExpression}")
    private String algorithmExpression;

    @Value("${app.shardingSphere.shardCount}")
    private Integer shardCount;

    private final DataSourcesConfig dataSourcesConfig;

    @Bean
    public DataSource createShardingDataSource() {
        final var chardMax = shardCount - 1;
        final var deviceTableRule = new ShardingTableRuleConfiguration(logicTable,
                SHARD_NAME + "${0.." + chardMax + "}." + logicTable
        );
        deviceTableRule.setDatabaseShardingStrategy(
                new StandardShardingStrategyConfiguration(shardingColumn, ALGORITHM_NAME)
        );

        final var shardingRule = new ShardingRuleConfiguration();
        shardingRule.getTables().add(deviceTableRule);
        final var props = new Properties();
        props.put("algorithm-expression", String.format(algorithmExpression, shardCount));
        shardingRule.getShardingAlgorithms().put(ALGORITHM_NAME, new AlgorithmConfiguration("INLINE", props));

        final var masterSource = dataSourcesConfig.getDataSources();
        masterSource.forEach(MigrationProcessor::runFlyway);

        final var dataSourceGroups = IntStream.range(0, chardMax)
                .mapToObj(i -> {
                    final var name = SHARD_NAME + i;
                    final var replicaName = name + REPLICA_POSTFIX;
                    return new ReadwriteSplittingDataSourceGroupRuleConfiguration(
                            name, name, Collections.singletonList(replicaName),
                            LOAD_BALANCERS_TYPE
                    );
                }).toList();


        final var loadBalancers = Map.of(
                LOAD_BALANCERS_TYPE, new AlgorithmConfiguration(LOAD_BALANCERS_TYPE.toUpperCase(), new Properties())
        );

        final var readwriteRule = new ReadwriteSplittingRuleConfiguration(dataSourceGroups, loadBalancers);

        final var dataSourceMap = getAllDataSources();

        try {
            return ShardingSphereDataSourceFactory.createDataSource(
                    dataSourceMap,
                    new HashSet<>(Arrays.asList(shardingRule, readwriteRule)),
                    new Properties()
            );
        } catch (SQLException e) {
            throw new DeviceServiceException(e);
        }
    }

    private Map<String, DataSource> getAllDataSources() {
        final var map = new HashMap<String, DataSource>();
        putDataSources(map, true);
        putDataSources(map, false);
        return map;
    }

    private void putDataSources(HashMap<String, DataSource> dataSourceMap, boolean isReplica) {
        final var sourceList = isReplica
                ? dataSourcesConfig.getReplicaDataSources()
                : dataSourcesConfig.getDataSources();
        for (int i = 0; i < sourceList.size(); i++) {
            final var name = isReplica ? SHARD_NAME + i + REPLICA_POSTFIX : SHARD_NAME + i;
            final var source = sourceList.get(i);
            final var ds = new HikariDataSource();
            ds.setJdbcUrl(source.getUrl());
            ds.setUsername(source.getUsername());
            ds.setPassword(source.getPassword());
            ds.setDriverClassName(POSTGRES_DRIVER);
            dataSourceMap.put(name, ds);
        }
    }

}
