CREATE TABLE IF NOT EXISTS users (
                                     id BIGSERIAL PRIMARY KEY,
                                     cognito_sub VARCHAR(255) NOT NULL UNIQUE,
                                     email VARCHAR(255) NOT NULL UNIQUE,
                                     name VARCHAR(255),
                                     role VARCHAR(50) NOT NULL DEFAULT 'USER',
                                     created_at TIMESTAMP(6) NOT NULL DEFAULT now(),
                                     updated_at TIMESTAMP(6) NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_cognito_sub ON users (cognito_sub);
CREATE INDEX IF NOT EXISTS idx_email ON users (email);

CREATE TABLE IF NOT EXISTS projects (
                                        id BIGSERIAL PRIMARY KEY,
                                        owner_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                        name VARCHAR(255) NOT NULL,
                                        description TEXT,
                                        created_at TIMESTAMP(6) NOT NULL DEFAULT now(),
                                        updated_at TIMESTAMP(6) NOT NULL DEFAULT now(),
                                        CONSTRAINT unique_project_name_per_owner UNIQUE (name, owner_id)
);

CREATE INDEX IF NOT EXISTS idx_projects_owner ON projects (owner_id);
CREATE INDEX IF NOT EXISTS idx_projects_name_owner ON projects (name, owner_id);

CREATE TABLE IF NOT EXISTS tasks (
                                     id BIGSERIAL PRIMARY KEY,
                                     project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                                     title VARCHAR(255) NOT NULL,
                                     description TEXT,
                                     status VARCHAR(50) NOT NULL,
                                     created_at TIMESTAMP(6) NOT NULL DEFAULT now(),
                                     updated_at TIMESTAMP(6) NOT NULL DEFAULT now(),
                                     CONSTRAINT unique_task_title_per_project UNIQUE (title, project_id)
);

CREATE INDEX IF NOT EXISTS idx_tasks_project ON tasks (project_id);
CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks (status);
CREATE INDEX IF NOT EXISTS idx_tasks_title_project ON tasks (title, project_id);

CREATE OR REPLACE FUNCTION trigger_set_timestamp()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

CREATE TRIGGER trg_projects_updated BEFORE UPDATE ON projects
    FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

CREATE TRIGGER trg_tasks_updated BEFORE UPDATE ON tasks
    FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();