package com.example.systrans.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
	private final String[] allowedOriginPatterns;

	public WebConfig(@Value("${app.cors.allowed-origin-patterns}") String allowedOrigins) {
		this.allowedOriginPatterns = Arrays.stream(allowedOrigins.split(","))
				.map(String::trim)
				.filter(origin -> !origin.isEmpty())
				.toArray(String[]::new);
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
				.allowedOriginPatterns(allowedOriginPatterns)
				.allowedMethods("GET", "POST", "OPTIONS")
				.allowedHeaders("Content-Type", "Authorization")
				.allowCredentials(true)
				.maxAge(3600);
	}
}
