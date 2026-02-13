package interfaces;

import java.time.Instant;
import java.util.Set;
import org.jboss.logging.Logger;

import application.UsuarioService;
import application.representation.UsuarioRepresentation;
import io.smallrye.jwt.build.Jwt;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@Path("/auth")
public class AuthResource {

    private static final Logger LOG = Logger.getLogger(AuthResource.class);

    @Inject
    private UsuarioService usuarioService;

    @GET
    @Path("/token")
    @jakarta.ws.rs.Produces(jakarta.ws.rs.core.MediaType.APPLICATION_JSON)
    public TokenResponse token(
            @QueryParam("user") String user,
            @QueryParam("password") String password
        ) {

        // Compara el usuario y password contra la base
        LOG.infof("Intento de login: user=%s, password=%s", user, password);
        UsuarioRepresentation usuario = usuarioService.findByUsuario(user);
        if (usuario != null) {
            LOG.infof("Usuario encontrado: %s, password en BD: %s, rol: %s", usuario.getUsuario(), usuario.getPassword(), usuario.getRol());
        } else {
            LOG.warnf("Usuario no encontrado: %s", user);
        }
        if (usuario != null && usuario.getPassword().equals(password)) {
            String issuer = "concesionario-auth";
            long ttl = 8000;
            Instant now = Instant.now();
            Instant exp = now.plusSeconds(ttl);
            String jwt = Jwt.issuer(issuer)
                    .subject(user)
                    .groups(Set.of(usuario.getRol())) // roles: user / admin
                    .issuedAt(now)
                    .expiresAt(exp)
                    .sign();
            LOG.infof("Login exitoso para usuario: %s", user);
            return new TokenResponse(jwt, exp.getEpochSecond(), usuario.getRol());
        } else {
            LOG.warnf("Login fallido para usuario: %s", user);
            throw new jakarta.ws.rs.WebApplicationException("Usuario o contraseña incorrectos", 401);
        }
    }

    public static class TokenResponse {
        public String accessToken;
        public long expiresAt;
        public String rol;

        public TokenResponse() {
        }

        public TokenResponse(String accessToken, long expiresAt, String role) {
            this.accessToken = accessToken;
            this.expiresAt = expiresAt;
            this.rol = role;
        }
    }

}