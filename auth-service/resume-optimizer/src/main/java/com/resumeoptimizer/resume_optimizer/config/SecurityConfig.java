package com.resumeoptimizer.resume_optimizer.config;

import com.resumeoptimizer.resume_optimizer.security.GithubOAuth2UserService;
import com.resumeoptimizer.resume_optimizer.security.OAuth2LoginSuccessHandler;
import com.resumeoptimizer.resume_optimizer.security.PromptAwareAuthorizationRequestResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * auth-service exposes public login/register/refresh endpoints plus the GitHub
 * OAuth2 login flow. Username/password endpoints are stateless (JWT); the
 * OAuth2 handshake uses a session only for the redirect dance, and the success
 * handler tears that session down as soon as our own tokens are issued.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final PromptAwareAuthorizationRequestResolver authorizationRequestResolver;
    private final GithubOAuth2UserService githubOAuth2UserService;

    /**
     * Read through Spring rather than System.getenv so it honours
     * application.properties, a Config Server or a -D override - the same
     * source the success handler uses.
     */
    @Value("${app.oauth2.redirect-uri:}")
    private String frontendRedirectUri;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CORS is handled once, at the gateway. Enabling it here as well
                // produces duplicate Access-Control-Allow-Origin headers, which
                // browsers reject outright.
                .cors(cors -> cors.disable())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/oauth2/**", "/login/**").permitAll()
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        // Forwards prompt=select_account so GitHub always shows
                        // the account picker.
                        .authorizationEndpoint(endpoint -> endpoint
                                .authorizationRequestResolver(authorizationRequestResolver))
                        // Fills in the verified email GitHub withholds by default.
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(githubOAuth2UserService))
                        .successHandler(oAuth2LoginSuccessHandler)
                        .failureHandler(this::onOAuthFailure))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout
                        .logoutUrl("/oauth2/logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"));

        return http.build();
    }

    /**
     * Sends the browser back to the frontend callback with an error flag rather
     * than rendering a Spring error page at an origin the user has never seen.
     */
    private void onOAuthFailure(
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response,
            org.springframework.security.core.AuthenticationException exception)
            throws java.io.IOException {

        log.warn("GitHub OAuth handshake failed: {}", exception.getMessage());

        String redirect = frontendRedirectUri == null ? "" : frontendRedirectUri;

        if (redirect.isBlank()) {
            response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED,
                    "GitHub sign-in failed");
            return;
        }

        response.sendRedirect(UriComponentsBuilder.fromUriString(redirect)
                .queryParam("error", "access_denied")
                .build()
                .toUriString());
    }
}
