package ts.andrey.eventcollector.configuration;

import com.datastax.oss.driver.api.core.CqlSession;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.net.InetSocketAddress;

@Configuration
@RequiredArgsConstructor
@Profile("!test")
public class CassandraInitConfiguration {

    private static final String CQL_INIT = "CREATE KEYSPACE IF NOT EXISTS iot_service "
            + "WITH replication = {'class':'SimpleStrategy','replication_factor':'1'};";

    @Value("${spring.cassandra.contact-points}")
    private String contactPoints;

    @Value("${spring.cassandra.port}")
    private int port;

    @Value("${spring.cassandra.local-datacenter}")
    private String localDatacenter;

    @PostConstruct
    public void init() {
        try (CqlSession session = CqlSession.builder()
                .addContactPoint(
                        new InetSocketAddress(contactPoints, port))
                .withLocalDatacenter(localDatacenter)
                .build()) {
            session.execute(CQL_INIT);
        }
    }

}
