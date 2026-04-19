--liquibase formatted sql
--changeset quizmi:20260419-1

ALTER TABLE quiz_sessions
    ADD answer_order_json TEXT NOT NULL DEFAULT '{}';
