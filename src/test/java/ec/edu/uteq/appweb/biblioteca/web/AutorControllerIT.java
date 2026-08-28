package ec.edu.uteq.appweb.biblioteca.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.uteq.appweb.biblioteca.BaseIntegracionTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Prueba de integracion HTTP de ejemplo YA IMPLEMENTADA, sobre el controlador
 * de referencia. Replique exactamente este patron para LibroController.
 *
 * Desde la Unidad IV el catalogo exige autenticacion (Bearer JWT), de modo que
 * estas llamadas se autentican contra /api/v1/auth/login y envian el token.
 */
class AutorControllerIT extends BaseIntegracionTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String obtenerToken(String username, String password) throws Exception {
        String cuerpo = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        MvcResult resultado = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode nodo = objectMapper.readTree(resultado.getResponse().getContentAsString());
        return nodo.path("data").path("token").asText();
    }

    @Test
    @DisplayName("GET /api/v1/autores responde 200 con el envoltorio ApiResponse y su meta")
    void listarAutoresDevuelveEnvoltorio() throws Exception {
        String token = obtenerToken("lector", "Lector123!");

        mockMvc.perform(get("/api/v1/autores").param("size", "5")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.meta.page").value(0))
                .andExpect(jsonPath("$.meta.size").value(5))
                .andExpect(jsonPath("$.meta.totalElements").isNumber());
    }

    @Test
    @DisplayName("GET de un autor inexistente responde 404 en formato ProblemDetail")
    void autorInexistenteDevuelveProblemDetail() throws Exception {
        String token = obtenerToken("lector", "Lector123!");

        mockMvc.perform(get("/api/v1/autores/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").exists());
    }
}
