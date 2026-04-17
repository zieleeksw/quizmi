--liquibase formatted sql
--changeset quizmi:20260416-1

ALTER TABLE question_versions
    ADD COLUMN explanation VARCHAR(2000);
