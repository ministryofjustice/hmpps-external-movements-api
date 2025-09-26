truncate audit_revision, temporary_absence_occurrence_audit, temporary_absence_occurrence, temporary_absence_authorisation_audit, temporary_absence_authorisation;
alter sequence audit_revision_id_seq restart with 1;

alter table temporary_absence_authorisation
    add column if not exists from_date date,
    add column if not exists to_date   date;

alter table temporary_absence_authorisation_audit
    add column if not exists from_date date,
    add column if not exists to_date   date;

alter table temporary_absence_authorisation
    alter column from_date set not null,
    alter column to_date set not null;

alter table temporary_absence_authorisation_audit
    alter column from_date set not null,
    alter column to_date set not null;

alter table temporary_absence_occurrence
    alter column accompanied_by_id set not null,
    alter column transport_id set not null;

alter table temporary_absence_occurrence_audit
    alter column accompanied_by_id set not null,
    alter column transport_id set not null;

