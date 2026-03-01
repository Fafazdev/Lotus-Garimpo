package lotus.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lotus.repositories.UsuarioRepository;
import lotus.model.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/home", "/login", "/cadastro", "/api/**",
                                "/css/**", "/js/**", "/imagens/**", "/static/**").permitAll()
                        .anyRequest().permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/")
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(oidcUserService())
                                .userService(oAuth2UserService())
                        )
                        .successHandler(googleSuccessHandler())
                );

        return http.build();
    }

    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        return new OidcUserService();
    }

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService() {
        return new DefaultOAuth2UserService();
    }

    @Bean
    public AuthenticationSuccessHandler googleSuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request,
                                                HttpServletResponse response,
                                                Authentication authentication) throws IOException {

                Object principal = authentication.getPrincipal();
                String email = null;
                String nome = null;

                if (principal instanceof OidcUser oidcUser) {
                    email = oidcUser.getEmail();
                    nome = oidcUser.getFullName();
                } else if (principal instanceof OAuth2User oAuth2User) {
                    Map<String, Object> attrs = oAuth2User.getAttributes();
                    Object emailAttr = attrs.get("email");
                    Object nameAttr = attrs.get("name");
                    email = emailAttr != null ? emailAttr.toString() : null;
                    nome = nameAttr != null ? nameAttr.toString() : null;
                }

                if (email != null) {
                    Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
                    Usuario usuario;

                    if (usuarioOpt.isPresent()) {
                        usuario = usuarioOpt.get();
                    } else {
                        usuario = new Usuario();
                        usuario.setEmail(email);
                        usuario.setNome(nome != null ? nome : "Usuário Google");
                        usuario.setTipo(1); // cliente por padrão
                        usuario.setDataNascimento(LocalDate.now());
                        usuarioRepository.save(usuario);
                    }

                    request.getSession().setAttribute("usuarioLogado", usuario);
                }

                response.sendRedirect("/perfil");
            }
        };
    }
}
