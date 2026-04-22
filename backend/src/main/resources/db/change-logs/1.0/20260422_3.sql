--liquibase formatted sql
--changeset quizmi:20260422-3

ALTER TABLE quiz_sessions
    ADD COLUMN checked_question_ids_json TEXT NOT NULL DEFAULT '[]';
