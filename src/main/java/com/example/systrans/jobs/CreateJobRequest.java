package com.example.systrans.jobs;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateJobRequest(
		@NotBlank @Size(max = 200) String title,
		@NotBlank @Size(max = 100) String department,
		@NotBlank @Size(max = 160) String location,
		@NotBlank @Size(max = 60) String employmentType,
		@NotBlank @Size(max = 40) String workplaceType,
		@NotBlank @Size(max = 100) String experienceLevel,
		@Size(max = 160) String salaryRange,
		@NotBlank @Size(max = 1000) String summary,
		@NotBlank String description,
		String responsibilities,
		@NotBlank String requirements,
		String benefits,
		@NotBlank @Email @Size(max = 254) String applicationEmail,
		@Size(max = 2048) @Pattern(regexp = "https?://\\S+", message = "must be an HTTP or HTTPS URL") String applicationUrl,
		LocalDate closingDate) {
}
