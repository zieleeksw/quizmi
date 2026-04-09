--liquibase formatted sql
--changeset quizmi:20260409-1

CREATE TABLE courses
(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    owner_user_id BIGINT NOT NULL,
    CONSTRAINT fk_courses_owner_user
        FOREIGN KEY (owner_user_id)
            REFERENCES users (id)
);
