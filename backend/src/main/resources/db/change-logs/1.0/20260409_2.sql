--liquibase formatted sql
--changeset quizmi:20260409-2

CREATE TABLE categories
(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    course_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_categories_course
        FOREIGN KEY (course_id)
            REFERENCES courses (id)
);
