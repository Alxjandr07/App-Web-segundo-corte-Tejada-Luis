package ec.edu.uteq.appweb.biblioteca;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base de las pruebas de integracion HTTP. YA IMPLEMENTADA: heredela y escriba
 * solo sus casos de prueba.
 *
 * Levanta un PostgreSQL 18 real en un contenedor efimero y deja que Flyway
 * aplique V1, V2 y V3 sobre el, de modo que cada ejecucion parte del mismo
 * estado conocido.
 *
 * Nota de entorno (Spring Boot 4 / Testcontainers 2): una sola instancia del
 * contenedor se arranca al cargar la clase (bloque estatico) y se comparte
 * durante toda la JVM de pruebas. Asi, cuando varias clases heredan de esta
 * base y la JVM las ejecuta en secuencia, todas reutilizan el mismo contenedor
 * y el mismo contexto de Spring (cache de contexto), evitando que el ciclo de
 * vida por clase de @Testcontainers detenga/relance el PostgreSQL entre clases
 * y deje huerfanos sus puertos. La conexion se expone con @DynamicPropertySource
 * (equivalente funcional a @ServiceConnection).
 *
 * Requisito: Docker debe estar corriendo en la maquina.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integracion")
public abstract class BaseIntegracionTest {

    @SuppressWarnings("rawtypes")
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:18-alpine")
                    .withDatabaseName("biblioteca_test")
                    .withUsername("test")
                    .withPassword("test");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void propiedadesBase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
