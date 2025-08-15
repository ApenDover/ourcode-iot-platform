package ts.andrey.eventcollector.configuration;

import com.datastax.oss.driver.api.core.CqlSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.config.SessionBuilderConfigurer;

@Slf4j
@Configuration
@RequiredArgsConstructor
@Profile("!test")
public class CassandraInitConfiguration extends AbstractCassandraConfiguration {

    private static final String CQL_INIT = "CREATE KEYSPACE IF NOT EXISTS %s "
            + "WITH replication = {%s};";

    @Value("${spring.cassandra.keyspace-name}")
    private String keyspace;

    @Value("${spring.cassandra.port}")
    private Integer port;

    @Value("${spring.cassandra.contact-points}")
    private String contactPoints;

    @Value("${spring.cassandra.replication-strategy}")
    private String replicationStrategy;

    @Override
    protected String getKeyspaceName() {
        return keyspace;
    }

    @Override
    protected int getPort() {
        return port;
    }

    @Override
    protected String getContactPoints() {
        return contactPoints;
    }

    @Override
    public SessionBuilderConfigurer getSessionBuilderConfigurer() {
        return cqlSessionBuilder -> {
            try (CqlSession tempSession = cqlSessionBuilder.build()) {
                tempSession.execute(String.format(CQL_INIT, keyspace, replicationStrategy));
            }
            return cqlSessionBuilder.withKeyspace(keyspace);
        };
    }

}
