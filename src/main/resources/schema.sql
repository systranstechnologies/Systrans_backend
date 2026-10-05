CREATE TABLE IF NOT EXISTS job_vacancies (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    department VARCHAR(100) NOT NULL,
    location VARCHAR(160) NOT NULL,
    employment_type VARCHAR(60) NOT NULL,
    workplace_type VARCHAR(40) NOT NULL,
    experience_level VARCHAR(100) NOT NULL,
    salary_range VARCHAR(160) NULL,
    summary VARCHAR(1000) NOT NULL,
    description LONGTEXT NOT NULL,
    responsibilities LONGTEXT NULL,
    requirements LONGTEXT NOT NULL,
    benefits LONGTEXT NULL,
    application_email VARCHAR(254) NOT NULL,
    application_url VARCHAR(2048) NULL,
    closing_date DATE NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
