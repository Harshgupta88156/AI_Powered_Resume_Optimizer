package com.resumeoptimizer.resume_optimizer.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeoptimizer.resume_optimizer.dto.AuthResponse;
import com.resumeoptimizer.resume_optimizer.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Runs after Spring Security completes the GitHub OAuth2 handshake: maps the
 * GitHub profile to (or creates) a user in user-service, mints our own JWT +
 * refresh token, and redirects to the frontend with them.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final String PROVIDER = "GITHUB";

    private final AuthService authService;
    private final ObjectMapper objectMapper;

    @Value("${app.oauth2.redirect-uri:}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

            String login = attr(oAuth2User, "login");
            String providerId = attr(oAuth2User, "id");
            String name = attr(oAuth2User, "name");
            String email = attr(oAuth2User, "email");
            String avatarUrl = attr(oAuth2User, "avatar_url");

            if (name == null || name.isBlank()) {
                name = login != null ? login : "GitHub User";
            }

            // GithubOAuth2UserService normally supplies the verified address.
            // This only fires if the /user/emails call failed.
            if (email == null || email.isBlank()) {
                email = (login != null ? login : "github-user") + "@users.noreply.github.com";
                log.warn("No verified GitHub email for '{}'; falling back to the no-reply address", login);
            }

            log.info("GitHub login: login={}, providerId={}", login, providerId);

            AuthResponse authResponse =
                    authService.oauthLogin(email, name, PROVIDER, providerId, avatarUrl);

            // The OAuth handshake needed a session; the app itself is stateless
            // JWT. Leaving it behind means the next /oauth2/authorization/github
            // hit can be short-circuited by the still-authenticated session,
            // which is one of the reasons switching accounts appeared to do
            // nothing.
            invalidateSession(request);

            if (redirectUri == null || redirectUri.isBlank()) {
                writeJson(response, authResponse);
                return;
            }

            String target = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("accessToken", authResponse.getAccessToken())
                    .queryParam("refreshToken", authResponse.getRefreshToken())
                    .queryParam("expiresInMs", authResponse.getExpiresInMs())
                    .build()
                    .toUriString();

            getRedirectStrategy().sendRedirect(request, response, target);

        } catch (Exception ex) {
            log.error("GitHub sign-in failed", ex);
            invalidateSession(request);

            // Hand the failure back to the frontend's callback route so it can
            // show a real message, instead of dumping a Whitelabel error page
            // on a user who is mid sign-in.
            if (redirectUri != null && !redirectUri.isBlank()) {
                String target = UriComponentsBuilder.fromUriString(redirectUri)
                        .queryParam("error", "oauth_failed")
                        .build()
                        .toUriString();
                getRedirectStrategy().sendRedirect(request, response, target);
                return;
            }

            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "OAuth authentication failed");
        }
    }

    private void invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    private String attr(OAuth2User user, String key) {
        Object value = user.getAttribute(key);
        return value != null ? value.toString() : null;
    }

    private void writeJson(HttpServletResponse response, AuthResponse authResponse) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(authResponse));
    }
}
