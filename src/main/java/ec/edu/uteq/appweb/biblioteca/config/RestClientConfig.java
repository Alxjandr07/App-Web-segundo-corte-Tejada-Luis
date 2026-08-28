package ec.edu.uteq.appweb.biblioteca.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP del lado del servidor para consumir la API externa.
 *
 * Los timeouts NO son opcionales: sin ellos, una API externa lenta bloquea los
 * hilos del servidor y termina tumbando la aplicacion propia. Es el fallo en
 * cascada que describe Nygard en Release It!.
 *
 * En Spring Boot 4 los timeouts por defecto de todos los clientes sincronos se
 * configuran de forma global con las propiedades spring.http.clients.connect-timeout
 * y spring.http.clients.read-timeout (ya declaradas en application.yml), y el
 * RestClient.Builder auto-configurado las aplica a cada cliente que se construya
 * a partir de el. De ahi que este bean solo fije la baseUrl de la API externa.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClientExterno(RestClient.Builder builder,
                                        @Value("${app.api-externa.base-url}") String baseUrl) {
        return builder
                .baseUrl(baseUrl)
                .build();
    }
}
