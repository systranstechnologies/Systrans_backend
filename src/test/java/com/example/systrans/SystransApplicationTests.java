package com.example.systrans;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
		"app.session.secret=test-session-secret-that-is-at-least-32-characters"
})
@AutoConfigureMockMvc
class SystransApplicationTests {

	@Autowired
	private MockMvc mockMvc;

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
				.andReturn();

		String cookie = login.getResponse().getCookie("sysTransAdmin").getValue();
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
	void creatingJobRequiresAnAdminCookie() throws Exception {
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
}
