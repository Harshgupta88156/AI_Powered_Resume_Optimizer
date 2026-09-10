package com.resumeoptimizer.resume_optimizer.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

/**
 * Passes {@code prompt} through to the provider's authorization endpoint.
 *
 * Without it, GitHub silently reuses whichever account is signed in on
 * github.com and bounces straight back — the user never sees a picker, so
 * "sign in with my other account" is impossible without logging out of GitHub
 * entirely. Forwarding {@code prompt=select_account} forces the picker.
 *
 * The value is allow-listed rather than passed through verbatim so a crafted
 * link cannot inject arbitrary parameters into the outbound request.
 */
@Component
public class PromptAwareAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private static final Set<String> ALLOWED_PROMPTS =
            Set.of("select_account", "login", "consent", "none");

    private final DefaultOAuth2AuthorizationRequestResolver delegate;

    public PromptAwareAuthorizationRequestResolver(ClientRegistrationRepository repository) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(
                repository, "/oauth2/authorization");
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return withPrompt(delegate.resolve(request), request);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return withPrompt(delegate.resolve(request, clientRegistrationId), request);
    }

    private OAuth2AuthorizationRequest withPrompt(
            OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request) {

        if (authorizationRequest == null) {
            return null;
        }

        String prompt = request.getParameter("prompt");
        // Default to the picker: switching accounts is the common case that was
        // broken, and re-selecting the same account is a single extra click.
        if (prompt == null || !ALLOWED_PROMPTS.contains(prompt)) {
            prompt = "select_account";
        }

        Map<String, Object> additional = new HashMap<>(authorizationRequest.getAdditionalParameters());
        additional.put("prompt", prompt);

        return OAuth2AuthorizationRequest.from(authorizationRequest)
                .additionalParameters(additional)
                .build();
    }
}
