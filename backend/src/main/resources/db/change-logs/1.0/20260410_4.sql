--liquibase formatted sql
--changeset quizmi:20260410-4

CREATE TABLE quiz_sessions
(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    course_id BIGINT NOT NULL,
    quiz_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    quiz_title VARCHAR(120) NOT NULL,
    question_ids_json TEXT NOT NULL,
    answers_json TEXT NOT NULL,
    current_index INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_quiz_sessions_course_quiz_user
        UNIQUE (course_id, quiz_id, user_id)
);
