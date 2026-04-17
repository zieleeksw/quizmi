--liquibase formatted sql
--changeset quizmi:20260417-1

ALTER TABLE question_answers
    ALTER COLUMN content TYPE VARCHAR(1000);
