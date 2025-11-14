alter table audit_revision
    add column if not exists reason text;