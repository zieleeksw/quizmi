--liquibase formatted sql
--changeset quizmi:20260421-1

CREATE INDEX idx_quiz_sessions_course_user_updated_at
    ON quiz_sessions (course_id, user_id, updated_at DESC);
