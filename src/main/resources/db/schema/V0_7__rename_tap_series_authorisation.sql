alter table temporary_absence_series
    rename to temporary_absence_authorisation;

alter table temporary_absence_series_audit
    rename to temporary_absence_authorisation_audit;

alter table temporary_absence_authorisation
    rename constraint pk_temporary_absence_series to pk_temporary_absence_authorisation;
alter table temporary_absence_authorisation
    rename constraint fk_temporary_absence_series_absence_type_id to fk_temporary_absence_authorisation_absence_type_id;
alter table temporary_absence_authorisation
    rename constraint fk_temporary_absence_series_absence_sub_type_id to fk_temporary_absence_authorisation_absence_sub_type_id;
alter table temporary_absence_authorisation
    rename constraint fk_temporary_absence_series_absence_reason_id to fk_temporary_absence_authorisation_absence_reason_id;
alter table temporary_absence_authorisation
    rename constraint fk_temporary_absence_series_accompanied_by_id to fk_temporary_absence_authorisation_accompanied_by_id;
alter table temporary_absence_authorisation
    rename constraint fk_temporary_absence_series_transport_id to fk_temporary_absence_authorisation_transport_id;
alter table temporary_absence_authorisation
    rename constraint fk_temporary_absence_series_status_id to fk_temporary_absence_authorisation_status_id;
alter table temporary_absence_authorisation
    rename constraint fk_temporary_absence_series_location_type_id to fk_temporary_absence_authorisation_location_type_id;
alter table temporary_absence_authorisation
    rename constraint uq_temporary_absence_series_legacy_id to uq_temporary_absence_authorisation_legacy_id;

alter index if exists idx_temporary_absence_series_person_identifier rename to idx_temporary_absence_authorisation_person_identifier;

alter table temporary_absence_authorisation_audit
    rename constraint pk_temporary_absence_series_audit to pk_temporary_absence_authorisation_audit;
alter table temporary_absence_authorisation_audit
    rename constraint temporary_absence_series_audit_rev_id_fkey to fk_temporary_absence_authorisation_audit_rev_id;

alter table temporary_absence_authorisation
    add column application_date date,
    add column submitted_by     varchar(64),
    add column approved_at      timestamp,
    add column approved_by      varchar(64),
    add column contact          text;

alter table temporary_absence_authorisation_audit
    add column application_date date,
    add column submitted_by     varchar(64),
    add column approved_at      timestamp,
    add column approved_by      varchar(64),
    add column contact          text;

alter table audit_revision
    drop column caseload_id;

truncate table audit_revision, temporary_absence_authorisation_audit, temporary_absence_authorisation;

alter table temporary_absence_authorisation
    alter column application_date set not null,
    alter column submitted_by set not null;

alter table temporary_absence_authorisation_audit
    alter column application_date set not null,
    alter column submitted_by set not null;