const apiBaseUrl = 'http://localhost:8080';

export const environment = {
  production: false,
  apiBaseUrl,

  /**
   * The OAuth handshake goes through the API gateway, not straight to
   * auth-service on :8082. Hitting the service directly means the browser talks
   * to two different origins during one sign-in, so the OAuth session cookie is
   * set on :8082 while the app runs on :4200 — which is why switching GitHub
   * accounts used to silently reuse the previous one.
   */
  githubOAuthUrl: `${apiBaseUrl}/oauth2/authorization/github`,

  /** Where auth-service sends the browser back to with the tokens. */
  oauthRedirectUri: 'http://localhost:4200/oauth2/callback'
};
