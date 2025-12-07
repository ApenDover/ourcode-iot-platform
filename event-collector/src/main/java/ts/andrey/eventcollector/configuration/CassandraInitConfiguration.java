package ts.andrey.eventcollector.configuration;

import com.datastax.oss.driver.api.core.CqlSession;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.cassandra.config.CqlSessionFactoryBean;
import org.springframework.data.cassandra.core.CassandraTemplate;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;

@Slf4j
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
    private List<String> contactPoints;

    @Value("${spring.cassandra.local-datacenter}")
    private String localDatacenter;

    @Value("${spring.cassandra.replication-strategy}")
    private String replicationStrategy;

    @Bean
    public CqlSessionFactoryBean session() {
        CqlSessionFactoryBean session = new CqlSessionFactoryBean();
        session.setContactPoints(cassandraPoints());
        session.setPort(port);
        session.setLocalDatacenter(localDatacenter);
        session.setKeyspaceCreations(Collections.emptyList());
        return session;
    }

    @Bean
    public CassandraTemplate cassandraTemplate(CqlSession session) {
        final var strategy = replicationStrategy.replaceAll("\"", "");
        final var connected = String.format(CQL_INIT, keyspace, strategy);
        log.info("Подключение к : {}", connected);
        session.execute(connected);
        session.execute("USE " + keyspace);
        return new CassandraTemplate(session);
    }

    @PostConstruct
    public void init() {
        try (CqlSession session = CqlSession.builder()
                .addContactPoints(cassandraPoints())
                .withLocalDatacenter(localDatacenter)
                .build()) {
            session.execute(String.format(CQL_INIT, keyspace, replicationStrategy));
        }
    }

    public List<InetSocketAddress> cassandraPoints() {
        return contactPoints.stream()
                .map(it -> new InetSocketAddress(it, port))
                .toList();
    }

}
