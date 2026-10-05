package com.example.systrans.jobs;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record JobResponse(
		long id,
		String title,
		String department,
		String location,
		String employmentType,
		String workplaceType,
		String experienceLevel,
		String salaryRange,
		String summary,
		String description,
		String responsibilities,
		String requirements,
		String benefits,
		String applicationEmail,
		String applicationUrl,
		LocalDate closingDate,
		LocalDateTime createdAt) {
}
