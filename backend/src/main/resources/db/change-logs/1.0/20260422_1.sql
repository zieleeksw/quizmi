--liquibase formatted sql
--changeset quizmi:20260422-1

CREATE INDEX idx_quiz_sessions_course_quiz_user
    ON quiz_sessions (course_id, quiz_id, user_id);
