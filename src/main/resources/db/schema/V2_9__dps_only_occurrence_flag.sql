alter table tap.occurrence
    add column if not exists dps_only boolean not null default false;

alter table tap.occurrence_audit
    add column if not exists dps_only boolean not null default false;

alter table tap.occurrence
    alter column dps_only drop default;

alter table tap.occurrence_audit
    alter column dps_only drop default;