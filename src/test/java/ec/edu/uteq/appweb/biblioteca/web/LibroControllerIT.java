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
 * Pruebas de integracion HTTP de LibroController (Parte B4 del examen).
 *
 * Replican el patron de AutorControllerIT y heredan de BaseIntegracionTest, que
 * levanta un PostgreSQL real en contenedor con las migraciones aplicadas.
 */
class LibroControllerIT extends BaseIntegracionTest {

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
    @DisplayName("GET /api/v1/libros responde 200 con las cinco claves del envoltorio y meta correcto")
    void listarLibrosDevuelveEnvoltorioConMeta() throws Exception {
        String token = obtenerToken("lector", "Lector123!");

        mockMvc.perform(get("/api/v1/libros").param("size", "5")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.meta").exists())
                .andExpect(jsonPath("$.meta.page").value(0))
                .andExpect(jsonPath("$.meta.size").value(5))
                .andExpect(jsonPath("$.meta.totalElements").isNumber());
    }

    @Test
    @DisplayName("GET /api/v1/libros/999999 responde 404 en formato ProblemDetail")
    void libroInexistenteDevuelveProblemDetail() throws Exception {
        String token = obtenerToken("lector", "Lector123!");

        mockMvc.perform(get("/api/v1/libros/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    @DisplayName("POST /api/v1/libros con titulo vacio responde 400 y el arreglo errors no esta vacio")
    void crearLibroConTituloVacioDevuelveValidacion() throws Exception {
        String token = obtenerToken("admin", "Admin123!");

        String cuerpoInvalido = "{"
                + "\"isbn\":\"9780134494166\","
                + "\"titulo\":\"\","
                + "\"anioPublicacion\":2024,"
                + "\"ejemplaresTotales\":3,"
                + "\"autorId\":1,"
                + "\"editorialId\":1,"
                + "\"categoriaId\":1"
                + "}";

        mockMvc.perform(post("/api/v1/libros")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0]").exists());
    }
}
