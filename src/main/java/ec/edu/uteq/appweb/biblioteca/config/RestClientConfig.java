package ec.edu.uteq.appweb.biblioteca.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Cliente HTTP del lado del servidor para consumir la API externa.
 *
 * Los timeouts NO son opcionales: sin ellos, una API externa lenta bloquea los
 * hilos del servidor y termina tumbando la aplicacion propia. Es el fallo en
 * cascada que describe Nygard en Release It!.
 *
 * En Spring Boot 4 el bean auto-configurado RestClient.Builder pertenece al
 * modulo spring-boot-restclient, que el starter webmvc no aporta. Por eso aqui
 * se construye el cliente de forma explicita con su propia fabrica de peticiones
 * y los timeouts declarados en app.api-externa.* del application.yml.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClientExterno(@Value("${app.api-externa.base-url}") String baseUrl,
                                        @Value("${app.api-externa.connect-timeout-ms}") int connectTimeoutMs,
                                        @Value("${app.api-externa.read-timeout-ms}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(connectTimeoutMs);
        fabrica.setReadTimeout(readTimeoutMs);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(fabrica)
                .build();
    }
}
