const apiBaseUrl = 'https://resume-optimizer-gateway.onrender.com';

export const environment = {
  production: true,
  apiBaseUrl,
  githubOAuthUrl: `${apiBaseUrl}/oauth2/authorization/github`,
  oauthRedirectUri: 'https://app.resumeoptimizer.com/oauth2/callback'
};
