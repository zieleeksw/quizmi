--liquibase formatted sql
--changeset quizmi:20260410-2

CREATE TABLE quizzes
(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    course_id BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    current_version_number INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_quizzes_course
        FOREIGN KEY (course_id)
            REFERENCES courses (id)
);

CREATE TABLE quiz_versions
(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    quiz_id BIGINT NOT NULL,
    version_number INTEGER NOT NULL,
    title VARCHAR(120) NOT NULL,
    mode VARCHAR(16) NOT NULL,
    random_count INTEGER,
    question_order VARCHAR(16) NOT NULL,
    answer_order VARCHAR(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_quiz_versions_quiz
        FOREIGN KEY (quiz_id)
            REFERENCES quizzes (id),
    CONSTRAINT uq_quiz_versions_quiz_version
        UNIQUE (quiz_id, version_number)
);

CREATE TABLE quiz_version_questions
(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    quiz_version_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    display_order INTEGER NOT NULL,
    CONSTRAINT fk_quiz_version_questions_quiz_version
        FOREIGN KEY (quiz_version_id)
            REFERENCES quiz_versions (id),
    CONSTRAINT fk_quiz_version_questions_question
        FOREIGN KEY (question_id)
            REFERENCES questions (id),
    CONSTRAINT uq_quiz_version_questions_version_order
        UNIQUE (quiz_version_id, display_order)
);

CREATE TABLE quiz_version_categories
(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    quiz_version_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    display_order INTEGER NOT NULL,
    CONSTRAINT fk_quiz_version_categories_quiz_version
        FOREIGN KEY (quiz_version_id)
            REFERENCES quiz_versions (id),
    CONSTRAINT fk_quiz_version_categories_category
        FOREIGN KEY (category_id)
            REFERENCES categories (id),
    CONSTRAINT uq_quiz_version_categories_version_order
        UNIQUE (quiz_version_id, display_order)
);
