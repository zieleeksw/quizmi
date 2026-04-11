--liquibase formatted sql
--changeset quizmi:20260410-3

CREATE TABLE quiz_attempts
(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    course_id BIGINT NOT NULL,
    quiz_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    quiz_title VARCHAR(120) NOT NULL,
    correct_answers INTEGER NOT NULL,
    total_questions INTEGER NOT NULL,
    review_snapshot_json TEXT NOT NULL,
    finished_at TIMESTAMP NOT NULL
);
