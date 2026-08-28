package ec.edu.uteq.appweb.biblioteca.integration;

import ec.edu.uteq.appweb.biblioteca.config.CacheConfig;
import ec.edu.uteq.appweb.biblioteca.exception.ServicioExternoException;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * ============================================================================
 * TODO-U4-4 (Objetivo especifico 3 de la Guia): CONSUMO DE API EXTERNA
 * ============================================================================
 *
 * Consume Open Library por ISBN con cache-aside en Redis y manejo diferenciado
 * de fallos:
 *   - 404 del proveedor  -> devuelve null (no es un error del sistema).
 *   - 4xx distinto de 404 -> ServicioExternoException.
 *   - 5xx                 -> ServicioExternoException.
 *   - timeout o fallo de red -> ServicioExternoException.
 * GlobalExceptionHandler convierte ServicioExternoException en un
 * ProblemDetail 502 Bad Gateway.
 *
 * NUNCA se cachea un fallo: los 5xx, 4xx y timeouts lanzan excepcion antes de
 * que la cache guarde nada, y los 404 devuelven null, que la cache no guarda
 * (disableCachingNullValues esta activo y ademas se refuerza con unless).
 */
@Component
public class OpenLibraryClient {

    private final RestClient restClient;

    public OpenLibraryClient(RestClient restClientExterno) {
        this.restClient = restClientExterno;
    }

    @Cacheable(cacheNames = CacheConfig.CACHE_OPENLIBRARY, key = "#isbn", unless = "#result == null")
    public OpenLibraryResponse consultarPorIsbn(String isbn) {
        try {
            ResponseEntity<OpenLibraryResponse> respuesta = restClient.get()
                    .uri("/isbn/{isbn}.json", isbn)
                    .retrieve()
                    .onStatus(estado -> estado.value() == 404, (peticion, r) -> { })
                    .onStatus(estado -> estado.value() != 404 && estado.value() >= 400,
                            (peticion, r) -> {
                                throw new ServicioExternoException(
                                        "El proveedor externo respondio " + r.getStatusCode()
                                                + " para el ISBN " + isbn);
                            })
                    .toEntity(OpenLibraryResponse.class);

            if (respuesta.getStatusCode().value() == 404) {
                return null;
            }
            return respuesta.getBody();
        } catch (ServicioExternoException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            throw new ServicioExternoException(
                    "No se pudo contactar al servicio externo para el ISBN " + isbn, ex);
        }
    }
}
