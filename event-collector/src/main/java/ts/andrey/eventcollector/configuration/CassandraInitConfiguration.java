package ts.andrey.eventcollector.configuration;

import com.datastax.oss.driver.api.core.CqlSession;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.cassandra.config.CqlSessionFactoryBean;
import org.springframework.data.cassandra.core.CassandraTemplate;

import java.net.InetSocketAddress;
import java.util.Collections;

@Configuration
@RequiredArgsConstructor
@Profile("!test")
public class CassandraInitConfiguration {

    private static final String CQL_INIT = "CREATE KEYSPACE IF NOT EXISTS %s "
            + "WITH replication = {%s};";

    @Value("${spring.cassandra.keyspace-name}")
    private String keyspace;

    @Value("${spring.cassandra.port}")
    private Integer port;

    @Value("${spring.cassandra.contact-points}")
    private String contactPoints;

    @Value("${spring.cassandra.local-datacenter}")
    private String localDatacenter;

    @Value("${spring.cassandra.replication-strategy}")
    private String replicationStrategy;

    @Bean
    public CqlSessionFactoryBean session() {
        CqlSessionFactoryBean session = new CqlSessionFactoryBean();
        session.setContactPoints(contactPoints);
        session.setPort(port);
        session.setLocalDatacenter(localDatacenter);
        session.setKeyspaceCreations(Collections.emptyList());
        return session;
    }

    @Bean
    public CassandraTemplate cassandraTemplate(CqlSession session) {
        session.execute(String.format(CQL_INIT, keyspace, replicationStrategy));
        session.execute("USE " + keyspace);
        return new CassandraTemplate(session);
    }

    @PostConstruct
    public void init() {
        try (CqlSession session = CqlSession.builder()
                .addContactPoint(
                        new InetSocketAddress(contactPoints, port))
                .withLocalDatacenter(localDatacenter)
                .build()) {
            session.execute(String.format(CQL_INIT, keyspace, replicationStrategy));
        }
    }

}
