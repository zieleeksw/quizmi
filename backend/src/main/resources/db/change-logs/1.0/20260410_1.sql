--liquibase formatted sql
--changeset quizmi:20260410-1

CREATE TABLE questions
(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    course_id BIGINT NOT NULL,
    current_version_number INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_questions_course
        FOREIGN KEY (course_id)
            REFERENCES courses (id)
);

CREATE TABLE question_versions
(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    question_id BIGINT NOT NULL,
    version_number INTEGER NOT NULL,
    prompt VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_question_versions_question
        FOREIGN KEY (question_id)
            REFERENCES questions (id),
    CONSTRAINT uq_question_versions_question_version
        UNIQUE (question_id, version_number)
);

CREATE TABLE question_answers
(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    question_version_id BIGINT NOT NULL,
    display_order INTEGER NOT NULL,
    content VARCHAR(300) NOT NULL,
    correct BOOLEAN NOT NULL,
    CONSTRAINT fk_question_answers_question_version
        FOREIGN KEY (question_version_id)
            REFERENCES question_versions (id),
    CONSTRAINT uq_question_answers_question_version_display_order
        UNIQUE (question_version_id, display_order)
);

CREATE TABLE question_version_categories
(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    question_version_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    display_order INTEGER NOT NULL,
    CONSTRAINT fk_question_version_categories_question_version
        FOREIGN KEY (question_version_id)
            REFERENCES question_versions (id),
    CONSTRAINT fk_question_version_categories_category
        FOREIGN KEY (category_id)
            REFERENCES categories (id),
    CONSTRAINT uq_question_version_categories_question_version_display_order
        UNIQUE (question_version_id, display_order)
);
