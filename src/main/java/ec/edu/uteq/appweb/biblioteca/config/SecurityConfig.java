package ec.edu.uteq.appweb.biblioteca.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.uteq.appweb.biblioteca.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.time.OffsetDateTime;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * ============================================================================
 * TODO-U4-2: CADENA DE SEGURIDAD
 * ============================================================================
 *
 * Estado final requerido:
 *   - csrf deshabilitado (la API es stateless y no usa formularios de sesion).
 *   - SessionCreationPolicy.STATELESS.
 *   - Publicos: POST /api/v1/auth/login, /swagger-ui/**, /v3/api-docs/**,
 *     /api/docs, /actuator/health.
 *   - El resto de /api/v1/** exige autenticacion.
 *   - Registrar JwtAuthenticationFilter antes de UsernamePasswordAuthenticationFilter.
 *   - Devolver 401 cuando no hay autenticacion y 403 cuando el rol no alcanza,
 *     ambos en formato ProblemDetail.
 *
 * La autorizacion fina por rol se declara con @PreAuthorize en los controladores,
 * habilitada por @EnableMethodSecurity.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(peticiones -> peticiones
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/api/docs",
                                "/actuator/health").permitAll()
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint((peticion, respuesta, ex) ->
                                escribirProblema(respuesta, HttpStatus.UNAUTHORIZED,
                                        "No autenticado", "Se requiere un token valido para acceder a este recurso",
                                        "no-autenticado"))
                        .accessDeniedHandler((peticion, respuesta, ex) ->
                                escribirProblema(respuesta, HttpStatus.FORBIDDEN,
                                        "Acceso denegado", "No tiene permisos suficientes para ejecutar esta operacion",
                                        "acceso-denegado")))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void escribirProblema(HttpServletResponse respuesta, HttpStatus estado,
                                  String titulo, String detalle, String tipo) throws IOException {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(titulo);
        problema.setType(URI.create("https://uteq.edu.ec/errores/" + tipo));
        problema.setProperty("timestamp", OffsetDateTime.now().toString());
        respuesta.setStatus(estado.value());
        respuesta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(respuesta.getOutputStream(), problema);
    }
}
