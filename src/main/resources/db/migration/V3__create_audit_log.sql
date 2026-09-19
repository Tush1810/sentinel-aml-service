-- Immutable trail of alert and case state transitions: who moved what, when and why.
-- Nothing in the AML workflow is deleted, so this table is append-only by construction.

CREATE TABLE audit_log (
    id          BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(16) NOT NULL,
    entity_ref  VARCHAR(48) NOT NULL,
    from_status VARCHAR(24),
    to_status   VARCHAR(24) NOT NULL,
    actor       VARCHAR(64) NOT NULL,
    reason      VARCHAR(1000),
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_audit_entity ON audit_log (entity_ref, occurred_at);

-- Immutability is enforced in the database, not only in application code: a regulator
-- has to be able to trust the trail even if something else gets a connection.
CREATE FUNCTION audit_log_append_only() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'audit_log is append-only; % is not permitted', tg_op;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_log_append_only
    BEFORE UPDATE OR DELETE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION audit_log_append_only();
