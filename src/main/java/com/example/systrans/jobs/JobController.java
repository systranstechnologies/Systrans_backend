package com.example.systrans.jobs;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.systrans.security.AdminLoginRateLimiter;
import com.example.systrans.security.AdminSessionService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class JobController {
	private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(JobController.class);

	private final JobService jobService;
	private final AdminSessionService adminSessionService;
	private final AdminLoginRateLimiter loginRateLimiter;
	private final boolean secureCookie;

	public JobController(
			JobService jobService,
			AdminSessionService adminSessionService,
			AdminLoginRateLimiter loginRateLimiter,
			@Value("${app.cookie.secure}") boolean secureCookie) {
		this.jobService = jobService;
		this.adminSessionService = adminSessionService;
		this.loginRateLimiter = loginRateLimiter;
		this.secureCookie = secureCookie;
	}

	@GetMapping("/jobs")
	public List<JobResponse> listJobs() {
		try {
			return jobService.listJobs();
		}
		catch (DataAccessException exception) {
			LOGGER.error("Could not load job vacancies", exception);
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Job vacancies are temporarily unavailable.");
		}
	}

	@GetMapping("/admin/session")
	public AdminSessionResponse adminSession(HttpServletRequest request) {
		return new AdminSessionResponse(adminSessionService.isAuthenticated(request));
	}

	@PostMapping("/admin/login")
	public ResponseEntity<AdminSessionResponse> login(
			@Valid @RequestBody AdminLoginRequest login,
			HttpServletRequest request) {
		String rateLimitKey = request.getRemoteAddr();
		if (!loginRateLimiter.allowAttempt(rateLimitKey)) {
			throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many sign-in attempts. Try again in 15 minutes.");
		}
		if (!adminSessionService.passwordMatches(login.password())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid admin password.");
		}
		loginRateLimiter.clear(rateLimitKey);
		return ResponseEntity.ok()
				.header("Set-Cookie", sessionCookie(adminSessionService.issueCookieValue(), adminSessionService.sessionDurationSeconds()).toString())
				.body(new AdminSessionResponse(true));
	}

	@PostMapping("/admin/logout")
	public ResponseEntity<AdminSessionResponse> logout() {
		return ResponseEntity.ok()
				.header("Set-Cookie", sessionCookie(adminSessionService.expiredCookieValue(), 0).toString())
				.body(new AdminSessionResponse(false));
	}

	@PostMapping("/jobs")
	public ResponseEntity<Map<String, Long>> createJob(
			@Valid @RequestBody CreateJobRequest job,
			HttpServletRequest request) {
		if (!adminSessionService.isAuthenticated(request)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
		}
		try {
			return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", jobService.createJob(job)));
		}
		catch (DataAccessException exception) {
			LOGGER.error("Could not save job vacancy", exception);
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "The vacancy could not be saved.");
		}
	}

	private ResponseCookie sessionCookie(String value, long maxAgeSeconds) {
		return ResponseCookie.from(adminSessionService.cookieName(), value)
				.httpOnly(true)
				.secure(secureCookie)
				.sameSite(secureCookie ? "None" : "Lax")
				.path("/")
				.maxAge(maxAgeSeconds)
				.build();
	}
}
