alter table if exists temporary_absence_authorisation
    drop column if exists application_date;

alter table if exists temporary_absence_authorisation_audit
    drop column if exists application_date;