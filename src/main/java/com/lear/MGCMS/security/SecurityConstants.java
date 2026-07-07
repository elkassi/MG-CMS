package com.lear.MGCMS.security;

public class SecurityConstants {

	public static final String SIGN_UP_URLS = "/api/user/login";
	public static final String H2_URL = "h2-console/**";
	/**
	 * JWT signing secret. Read from the {@code MGCMS_JWT_SECRET} environment
	 * variable when set, otherwise the historical literal so behaviour is
	 * unchanged in dev. Set a strong secret (>= 64 chars for HS512) in prod via
	 * the env var — no application.properties change needed. Rotating it
	 * invalidates existing tokens (users simply re-login).
	 */
	public static final String SECRET = resolveSecret();
	public static final String TOKEN_PREFIX = "Bearer ";
	public static final String HEADER_STRING = "Authorization";
	public static final long EXPIRATION_TIME = 1000 * 60 * 60 * 24 * 7;

	private static String resolveSecret() {
		String env = System.getenv("MGCMS_JWT_SECRET");
		return (env != null && !env.trim().isEmpty()) ? env.trim() : "SecretKeyToGenJWTs";
	}

}
