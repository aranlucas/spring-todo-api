CREATE SEQUENCE todos_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE todos (
    id BIGINT NOT NULL PRIMARY KEY,
    content VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL
);

CREATE INDEX idx_todos_email_id ON todos (email, id);
