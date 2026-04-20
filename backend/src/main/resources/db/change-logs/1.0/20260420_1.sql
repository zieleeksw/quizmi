--liquibase formatted sql
--changeset quizmi:20260420-1

CREATE INDEX idx_quizzes_course_active_created_at
    ON quizzes (course_id, active, created_at DESC);

CREATE INDEX idx_questions_course_created_at
    ON questions (course_id, created_at DESC);
