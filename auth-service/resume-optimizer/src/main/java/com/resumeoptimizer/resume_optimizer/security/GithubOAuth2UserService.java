package com.resumeoptimizer.resume_optimizer.security;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Enriches the GitHub profile with the user's verified primary email.
 *
 * GitHub's /user endpoint returns {@code "email": null} whenever the user has
 * not made an address public — which is the default. The success handler used
 * to paper over that by inventing
 * {@code {login}@users.noreply.github.com}, so two different private accounts
 * could not be told apart reliably and the "wrong account" bug followed.
 *
 * /user/emails (granted by the user:email scope) returns the real addresses.
 * If it fails we still fall back to the no-reply address rather than blocking
 * the login.
 */
@Service
@Slf4j
public class GithubOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private static final String EMAILS_URI = "https://api.github.com/user/emails";

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    private final RestClient restClient = RestClient.create();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User user = delegate.loadUser(userRequest);

        Map<String, Object> attributes = new HashMap<>(user.getAttributes());
        Object existingEmail = attributes.get("email");

        if (existingEmail == null || existingEmail.toString().isBlank()) {
            fetchPrimaryEmail(userRequest.getAccessToken().getTokenValue())
                    .ifPresent(email -> attributes.put("email", email));
        }

        String nameAttributeKey = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        return new DefaultOAuth2User(
                new ArrayList<>(user.getAuthorities()),
                attributes,
                nameAttributeKey);
    }

    private java.util.Optional<String> fetchPrimaryEmail(String accessToken) {
        try {
            List<Map<String, Object>> emails = restClient.method(HttpMethod.GET)
                    .uri(EMAILS_URI)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.ACCEPT, "application/vnd.github+json")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});

            if (emails == null || emails.isEmpty()) {
                return java.util.Optional.empty();
            }

            // Prefer the verified primary; settle for any verified address.
            return emails.stream()
                    .filter(e -> Boolean.TRUE.equals(e.get("primary")) && Boolean.TRUE.equals(e.get("verified")))
                    .map(e -> (String) e.get("email"))
                    .findFirst()
                    .or(() -> emails.stream()
                            .filter(e -> Boolean.TRUE.equals(e.get("verified")))
                            .map(e -> (String) e.get("email"))
                            .findFirst());

        } catch (Exception ex) {
            // Not fatal: the success handler falls back to the no-reply address.
            log.warn("Could not read the GitHub email list: {}", ex.getMessage());
            return java.util.Optional.empty();
        }
    }
}
