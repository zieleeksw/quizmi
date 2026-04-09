--liquibase formatted sql
--changeset quizmi:20260409-3

ALTER TABLE categories
    ADD COLUMN updated_at TIMESTAMP;

UPDATE categories
SET updated_at = created_at
WHERE updated_at IS NULL;

ALTER TABLE categories
    ALTER COLUMN updated_at SET NOT NULL;

CREATE TABLE category_versions
(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category_id BIGINT NOT NULL,
    version_number INT NOT NULL,
    name VARCHAR(120) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_category_versions_category
        FOREIGN KEY (category_id)
            REFERENCES categories (id),
    CONSTRAINT uq_category_versions_category_version
        UNIQUE (category_id, version_number)
);

INSERT INTO category_versions (category_id, version_number, name, created_at)
SELECT id, 1, name, created_at
FROM categories;
