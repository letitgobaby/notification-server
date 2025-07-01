package notification.adapter.db;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class MariadbTestContainerConfig {

    @Container
    protected static final MariaDBContainer<?> MARIADB_CONTAINER = new MariaDBContainer<>("mariadb:10.5")
            .withDatabaseName("test-db")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true);

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        String host = MARIADB_CONTAINER.getHost();
        Integer port = MARIADB_CONTAINER.getMappedPort(3306);
        String db = MARIADB_CONTAINER.getDatabaseName();

        // JDBC for Flyway
        registry.add("spring.flyway.url", () -> "jdbc:mariadb://" + host + ":" + port + "/" + db);
        registry.add("spring.flyway.user", MARIADB_CONTAINER::getUsername);
        registry.add("spring.flyway.password", MARIADB_CONTAINER::getPassword);

        // R2DBC for Repository
        registry.add("spring.r2dbc.url", () -> "r2dbc:mariadb://" + host + ":" + port + "/" + db);
        registry.add("spring.r2dbc.username", MARIADB_CONTAINER::getUsername);
        registry.add("spring.r2dbc.password", MARIADB_CONTAINER::getPassword);
    }
}
