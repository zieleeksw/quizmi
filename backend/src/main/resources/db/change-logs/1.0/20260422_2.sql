--liquibase formatted sql
--changeset quizmi:20260422-2

ALTER TABLE quiz_sessions
    ADD COLUMN furthest_index INTEGER NOT NULL DEFAULT 0;
