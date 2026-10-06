package com.example.systrans.jobs;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.PreparedStatement;

@Service
public class JobService {
	private static final String SELECT_JOBS = """
			SELECT id, title, department, location, employment_type, workplace_type,
			       experience_level, salary_range, summary, description, responsibilities,
			       requirements, benefits, application_email, application_url, closing_date,
			       created_at
			FROM job_vacancies
			ORDER BY created_at DESC, id DESC
			""";

	private static final RowMapper<JobResponse> JOB_ROW_MAPPER = (result, rowNumber) -> new JobResponse(
			result.getLong("id"),
			result.getString("title"),
			result.getString("department"),
			result.getString("location"),
			result.getString("employment_type"),
			result.getString("workplace_type"),
			result.getString("experience_level"),
			result.getString("salary_range"),
			result.getString("summary"),
			result.getString("description"),
			result.getString("responsibilities"),
			result.getString("requirements"),
			result.getString("benefits"),
			result.getString("application_email"),
			result.getString("application_url"),
			result.getObject("closing_date", java.time.LocalDate.class),
			result.getTimestamp("created_at").toLocalDateTime());

	private final JdbcTemplate jdbcTemplate;

	public JobService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<JobResponse> listJobs() {
		return jdbcTemplate.query(SELECT_JOBS, JOB_ROW_MAPPER);
	}

	public long createJob(CreateJobRequest request) {
		String sql = """
				INSERT INTO job_vacancies (
				    title, department, location, employment_type, workplace_type,
				    experience_level, salary_range, summary, description, responsibilities,
				    requirements, benefits, application_email, application_url, closing_date
				) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
				""";
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			PreparedStatement statement = connection.prepareStatement(sql, new String[] { "id" });
			statement.setString(1, request.title().trim());
			statement.setString(2, request.department().trim());
			statement.setString(3, request.location().trim());
			statement.setString(4, request.employmentType().trim());
			statement.setString(5, request.workplaceType().trim());
			statement.setString(6, request.experienceLevel().trim());
			statement.setString(7, nullableText(request.salaryRange()));
			statement.setString(8, request.summary().trim());
			statement.setString(9, request.description().trim());
			statement.setString(10, nullableText(request.responsibilities()));
			statement.setString(11, request.requirements().trim());
			statement.setString(12, nullableText(request.benefits()));
			statement.setString(13, request.applicationEmail().trim());
			statement.setString(14, nullableText(request.applicationUrl()));
			statement.setObject(15, request.closingDate());
			return statement;
		}, keyHolder);
		Number id = keyHolder.getKey();
		if (id == null) {
			throw new IllegalStateException("The database did not return an id for the saved job.");
		}
		return id.longValue();
	}

	private static String nullableText(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}
