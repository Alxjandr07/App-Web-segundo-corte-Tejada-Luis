package ec.edu.uteq.appweb.biblioteca.web.controller;

import ec.edu.uteq.appweb.biblioteca.domain.Usuario;
import ec.edu.uteq.appweb.biblioteca.repository.UsuarioRepository;
import ec.edu.uteq.appweb.biblioteca.security.JwtService;
import ec.edu.uteq.appweb.biblioteca.web.dto.ApiResponse;
import ec.edu.uteq.appweb.biblioteca.web.dto.LoginRequest;
import ec.edu.uteq.appweb.biblioteca.web.dto.LoginResponse;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TODO-U4-2: autenticacion.
 *
 *   POST /api/v1/auth/login   recibe LoginRequest, valida con BCrypt contra
 *                             UsuarioRepository.findByUsernameAndActivoTrue,
 *                             y devuelve LoginResponse dentro de ApiResponse.
 *                             El token va en el campo data.token de la respuesta.
 *   POST /api/v1/auth/logout  invalida el token por su jti (opcional, suma en la rubrica).
 *
 * Credenciales sembradas por Flyway en V3__usuarios.sql:
 *   admin / Admin123!          rol ADMIN
 *   bibliotecario / Biblio123! rol BIBLIOTECARIO
 *   lector / Lector123!        rol LECTOR
 *
 * Un login fallido debe devolver 401 en formato ProblemDetail, no 200 con success=false.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UsuarioRepository usuarios,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest solicitud) {
        Usuario usuario = usuarios.findByUsernameAndActivoTrue(solicitud.username()).orElse(null);
        if (usuario == null || !passwordEncoder.matches(solicitud.password(), usuario.getPasswordHash())) {
            ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                    HttpStatus.UNAUTHORIZED, "Usuario o contrasena invalidos");
            problema.setTitle("Credenciales incorrectas");
            problema.setType(URI.create("https://uteq.edu.ec/errores/credenciales-invalidas"));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problema);
        }

        String token = jwtService.generar(usuario);
        LoginResponse cuerpo = new LoginResponse(
                usuario.getUsername(),
                usuario.getRol().name(),
                token,
                "Bearer",
                jwtService.expiracionEnSegundos());
        return ResponseEntity.ok(ApiResponse.ok(cuerpo, "Sesion iniciada"));
    }
}
