CREATE TABLE audit_logs (
    id           BIGSERIAL PRIMARY KEY,
    module       VARCHAR(50)  NOT NULL,
    action       VARCHAR(50)  NOT NULL,
    entity_id    BIGINT,
    entity_name  VARCHAR(300),
    performed_by VARCHAR(100) NOT NULL,
    details      TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_module      ON audit_logs (module);
CREATE INDEX idx_audit_action      ON audit_logs (action);
CREATE INDEX idx_audit_performed   ON audit_logs (performed_by);
CREATE INDEX idx_audit_created     ON audit_logs (created_at DESC);
CREATE INDEX idx_audit_entity      ON audit_logs (module, entity_id);
