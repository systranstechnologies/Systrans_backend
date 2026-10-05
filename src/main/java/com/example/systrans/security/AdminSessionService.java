package com.example.systrans.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AdminSessionService {
	private static final String COOKIE_NAME = "sysTransAdmin";
	private static final long SESSION_DURATION_SECONDS = 8 * 60 * 60;

	private final byte[] passwordHash;
	private final String sessionSecret;

	public AdminSessionService(
			@Value("${app.admin.password}") String adminPassword,
			@Value("${app.session.secret}") String sessionSecret) {
		if (adminPassword.isBlank()) {
			throw new IllegalStateException("ADMIN_PASSWORD must be configured.");
		}
		if (sessionSecret.length() < 32) {
			throw new IllegalStateException("SESSION_SECRET must contain at least 32 characters.");
		}
		this.passwordHash = sha256(adminPassword);
		this.sessionSecret = sessionSecret;
	}

	public boolean passwordMatches(String providedPassword) {
		return providedPassword != null
				&& MessageDigest.isEqual(passwordHash, sha256(providedPassword));
	}

	public String issueCookieValue() {
		long expiration = Instant.now().getEpochSecond() + SESSION_DURATION_SECONDS;
		return expiration + "." + sign(expiration);
	}

	public boolean isAuthenticated(HttpServletRequest request) {
		String authorizationHeader = request.getHeader("Authorization");
		if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
			return isValidToken(authorizationHeader.substring("Bearer ".length()).trim());
		}

		String cookieHeader = request.getHeader("Cookie");
		if (cookieHeader == null || cookieHeader.isBlank()) {
			return false;
		}
		for (String cookie : cookieHeader.split(";")) {
			String trimmedCookie = cookie.trim();
			String prefix = COOKIE_NAME + "=";
			if (trimmedCookie.startsWith(prefix)) {
				return isValidToken(trimmedCookie.substring(prefix.length()));
			}
		}
		return false;
	}

	public String expiredCookieValue() {
		return "";
	}

	public String cookieName() {
		return COOKIE_NAME;
	}

	public long sessionDurationSeconds() {
		return SESSION_DURATION_SECONDS;
	}

	private boolean isValidToken(String token) {
		String[] parts = token.split("\\.", -1);
		if (parts.length != 2 || !parts[0].matches("\\d+") || !parts[1].matches("[a-fA-F0-9]{64}")) {
			return false;
		}
		try {
			long expiration = Long.parseLong(parts[0]);
			if (expiration <= Instant.now().getEpochSecond()) {
				return false;
			}
			byte[] providedSignature = HexFormat.of().parseHex(parts[1]);
			byte[] expectedSignature = HexFormat.of().parseHex(sign(expiration));
			return MessageDigest.isEqual(expectedSignature, providedSignature);
		}
		catch (IllegalArgumentException exception) {
			return false;
		}
	}

	private String sign(long expiration) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(sessionSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return HexFormat.of().formatHex(mac.doFinal(Long.toString(expiration).getBytes(StandardCharsets.UTF_8)));
		}
		catch (Exception exception) {
			throw new IllegalStateException("Could not sign admin session.", exception);
		}
	}

	private static byte[] sha256(String value) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
		}
		catch (Exception exception) {
			throw new IllegalStateException("SHA-256 is not available.", exception);
		}
	}
}
