package ec.edu.uteq.appweb.biblioteca.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * ============================================================================
 * TODO-U4-2: FILTRO QUE AUTENTICA CADA PETICION A PARTIR DEL JWT
 * ============================================================================
 *
 * En este orden:
 *   1. Lee el token de la cabecera Authorization: Bearer &lt;token&gt;
 *      (o tambien de una cookie HttpOnly llamada access_token).
 *   2. Si no hay token, deja pasar la peticion sin autenticar: este filtro NO
 *      rechaza, de eso se encarga la cadena de seguridad.
 *   3. Si hay token y es valido, construye un UsernamePasswordAuthenticationToken
 *      con las autoridades derivadas del claim rol, prefijadas con "ROLE_",
 *      y lo coloca en el SecurityContextHolder.
 *   4. Si el token es invalido o expiro, limpia el contexto y continua.
 *
 * Nunca escribe la respuesta de error aqui dentro: eso romperia el contrato de
 * ProblemDetail que ya implementa GlobalExceptionHandler.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion,
                                    HttpServletResponse respuesta,
                                    FilterChain cadena) throws ServletException, IOException {
        String token = extraerToken(peticion);

        if (token != null && jwtService.esValido(token)) {
            String username = jwtService.extraerUsername(token);
            String rol = jwtService.extraerRol(token);
            var autoridades = List.of(new SimpleGrantedAuthority("ROLE_" + rol));
            var autenticacion = new UsernamePasswordAuthenticationToken(username, null, autoridades);
            SecurityContextHolder.getContext().setAuthentication(autenticacion);
        } else {
            SecurityContextHolder.clearContext();
        }

        cadena.doFilter(peticion, respuesta);
    }

    private String extraerToken(HttpServletRequest peticion) {
        String cabecera = peticion.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(cabecera) && cabecera.startsWith("Bearer ")) {
            return cabecera.substring(7);
        }
        if (peticion.getCookies() != null) {
            for (Cookie galleta : peticion.getCookies()) {
                if ("access_token".equals(galleta.getName()) && StringUtils.hasText(galleta.getValue())) {
                    return galleta.getValue();
                }
            }
        }
        return null;
    }
}
