--liquibase formatted sql
--changeset quizmi:20260415-1

CREATE TABLE course_memberships
(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    course_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_course_memberships_course
        FOREIGN KEY (course_id)
            REFERENCES courses (id),
    CONSTRAINT fk_course_memberships_user
        FOREIGN KEY (user_id)
            REFERENCES users (id),
    CONSTRAINT uq_course_memberships_course_user
        UNIQUE (course_id, user_id)
);

CREATE INDEX idx_course_memberships_course_status
    ON course_memberships (course_id, status);

INSERT INTO course_memberships (course_id, user_id, role, status, created_at, updated_at)
SELECT id, owner_user_id, 'OWNER', 'ACTIVE', created_at, created_at
FROM courses;
