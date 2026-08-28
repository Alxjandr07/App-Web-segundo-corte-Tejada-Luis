package ec.edu.uteq.appweb.biblioteca.security;

import ec.edu.uteq.appweb.biblioteca.domain.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * ============================================================================
 * TODO-U4-2 (Objetivo especifico 2 de la Guia): AUTENTICACION JWT STATELESS
 * ============================================================================
 *
 * Emision y validacion del token con la libreria jjwt 0.13.0.
 *
 * Contenido del token:
 *   - sub  : el username
 *   - rol  : el rol del usuario (ADMIN, BIBLIOTECARIO o LECTOR)
 *   - jti  : identificador unico del token (UUID), necesario para revocarlo
 *   - iat  : fecha de emision
 *   - exp  : fecha de expiracion, tomada de app.jwt.expiracion-minutos
 *
 * Firma: HMAC-SHA256 con la clave de app.jwt.secreto (inyectada por variable
 * de entorno, nunca versionada).
 */
@Service
public class JwtService {

    private final String secretoBase64;
    private final long expiracionMinutos;

    public JwtService(@Value("${app.jwt.secreto}") String secretoBase64,
                      @Value("${app.jwt.expiracion-minutos}") long expiracionMinutos) {
        if (secretoBase64 == null || secretoBase64.isBlank()) {
            throw new IllegalStateException(
                    "app.jwt.secreto no esta definido. Generelo con 'openssl rand -base64 48' y expórtelo.");
        }
        this.secretoBase64 = secretoBase64;
        this.expiracionMinutos = expiracionMinutos;
    }

    private SecretKey clave() {
        return Keys.hmacShaKeyFor(secretoBase64.getBytes(StandardCharsets.UTF_8));
    }

    public String generar(Usuario usuario) {
        Instant ahora = Instant.now();
        Instant expiracion = ahora.plus(expiracionMinutos, ChronoUnit.MINUTES);
        return Jwts.builder()
                .subject(usuario.getUsername())
                .claim("rol", usuario.getRol().name())
                .id(java.util.UUID.randomUUID().toString())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(expiracion))
                .signWith(clave())
                .compact();
    }

    public Claims reclamaciones(String token) {
        return Jwts.parser()
                .verifyWith(clave())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extraerUsername(String token) {
        return reclamaciones(token).getSubject();
    }

    public String extraerRol(String token) {
        return reclamaciones(token).get("rol", String.class);
    }

    public String extraerJti(String token) {
        return reclamaciones(token).getId();
    }

    public boolean esValido(String token) {
        try {
            reclamaciones(token);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    public long expiracionEnSegundos() {
        return expiracionMinutos * 60;
    }
}
