-- Department master
CREATE TABLE departments (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(150) NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(100),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by  VARCHAR(100),
    CONSTRAINT uq_department_name UNIQUE (name)
);

-- Seed a few common departments
INSERT INTO departments (name, active, created_by)
VALUES ('Administration', TRUE, 'system'),
       ('Logistics', TRUE, 'system'),
       ('Accounts', TRUE, 'system'),
       ('Operations', TRUE, 'system');

-- Link users to departments (optional)
ALTER TABLE users ADD COLUMN department_id BIGINT REFERENCES departments(id) ON DELETE SET NULL;
