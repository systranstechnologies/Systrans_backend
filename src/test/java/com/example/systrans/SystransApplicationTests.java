package com.example.systrans;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:systrans;MODE=MySQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.sql.init.mode=never",
		"app.admin.password=test-admin-password",
		"app.cookie.secure=true",
		"app.session.secret=test-session-secret-that-is-at-least-32-characters"
})
@AutoConfigureMockMvc
class SystransApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void createJobsTable() {
		jdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS job_vacancies (
				    id BIGINT AUTO_INCREMENT PRIMARY KEY,
				    title VARCHAR(200) NOT NULL,
				    department VARCHAR(100) NOT NULL,
				    location VARCHAR(160) NOT NULL,
				    employment_type VARCHAR(60) NOT NULL,
				    workplace_type VARCHAR(40) NOT NULL,
				    experience_level VARCHAR(100) NOT NULL,
				    salary_range VARCHAR(160),
				    summary VARCHAR(1000) NOT NULL,
				    description CLOB NOT NULL,
				    responsibilities CLOB,
				    requirements CLOB NOT NULL,
				    benefits CLOB,
				    application_email VARCHAR(254) NOT NULL,
				    application_url VARCHAR(2048),
				    closing_date DATE,
				    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
				)
				""");
	}

	@Test
	void contextLoads() {
	}

	@Test
	void adminCanLoginAndReadAuthenticatedSession() throws Exception {
		MvcResult login = mockMvc.perform(post("/api/admin/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"password":"test-admin-password"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.authenticated").value(true))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
						.string("Set-Cookie", org.hamcrest.Matchers.allOf(
								org.hamcrest.Matchers.containsString("SameSite=None"),
								org.hamcrest.Matchers.containsString("Secure"))))
				.andReturn();

		String cookie = login.getResponse().getCookie("sysTransAdmin").getValue();
		java.util.regex.Matcher tokenMatcher = java.util.regex.Pattern
				.compile("\"token\":\"([^\"]+)\"")
				.matcher(login.getResponse().getContentAsString());
		org.junit.jupiter.api.Assertions.assertTrue(tokenMatcher.find());
		String token = tokenMatcher.group(1);
		org.junit.jupiter.api.Assertions.assertTrue(token.contains("."));
		mockMvc.perform(get("/api/admin/session").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.authenticated").value(true));

		mockMvc.perform(get("/api/admin/session").cookie(login.getResponse().getCookie("sysTransAdmin")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.authenticated").value(true));

		mockMvc.perform(post("/api/admin/logout").cookie(login.getResponse().getCookie("sysTransAdmin")))
				.andExpect(status().isOk())
				.andExpect(cookie().maxAge("sysTransAdmin", 0));

		mockMvc.perform(get("/api/admin/session").header("Cookie", "sysTransAdmin=" + cookie))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.authenticated").value(true));
	}

	@Test
	void invalidAdminPasswordIsRejected() throws Exception {
		mockMvc.perform(post("/api/admin/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"password":"wrong-password"}
						"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void creatingJobRequiresAnAdminSession() throws Exception {
		mockMvc.perform(post("/api/jobs")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "title":"Engineer",
						  "department":"Engineering",
						  "location":"Remote",
						  "employmentType":"Full-time",
						  "workplaceType":"Remote",
						  "experienceLevel":"Mid-level",
						  "summary":"Build software.",
						  "description":"Build software.",
						  "requirements":"Experience required.",
						  "applicationEmail":"jobs@example.com"
						}
						"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void adminCanCreateJobUsingBearerToken() throws Exception {
		MvcResult login = mockMvc.perform(post("/api/admin/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"password":"test-admin-password"}
						"""))
				.andExpect(status().isOk())
				.andReturn();
		java.util.regex.Matcher tokenMatcher = java.util.regex.Pattern
				.compile("\"token\":\"([^\"]+)\"")
				.matcher(login.getResponse().getContentAsString());
		org.junit.jupiter.api.Assertions.assertTrue(tokenMatcher.find());

		mockMvc.perform(post("/api/jobs")
				.header("Authorization", "Bearer " + tokenMatcher.group(1))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "title":"Engineer",
						  "department":"Engineering",
						  "location":"Remote",
						  "employmentType":"Full-time",
						  "workplaceType":"Remote",
						  "experienceLevel":"Mid-level",
						  "summary":"Build software.",
						  "description":"Build software.",
						  "requirements":"Experience required.",
						  "applicationEmail":"jobs@example.com"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber());
	}

	@Test
	void corsAllowsBearerAuthorizationHeader() throws Exception {
		mockMvc.perform(options("/api/admin/session")
				.header("Origin", "https://example.netlify.app")
				.header("Access-Control-Request-Method", "GET")
				.header("Access-Control-Request-Headers", "authorization,content-type"))
				.andExpect(status().isOk())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
						.string("Access-Control-Allow-Headers",
								org.hamcrest.Matchers.containsStringIgnoringCase("authorization")));
	}
}
