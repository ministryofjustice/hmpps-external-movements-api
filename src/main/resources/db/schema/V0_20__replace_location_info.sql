alter table temporary_absence_authorisation
    add column if not exists schedule jsonb;

alter table temporary_absence_authorisation_audit
    add column if not exists schedule jsonb;

alter table temporary_absence_occurrence
    add column if not exists location           jsonb,
    add column if not exists schedule_reference jsonb,
    drop column location_id,
    drop column location_type_id;

alter table temporary_absence_occurrence_audit
    add column if not exists location           jsonb,
    add column if not exists schedule_reference jsonb,
    drop column location_id,
    drop column location_type_id;

alter table temporary_absence_occurrence
    rename column contact to contact_information;

alter table temporary_absence_occurrence_audit
    rename column contact to contact_information;

alter table temporary_absence_movement
    drop column location_type_id;

alter table temporary_absence_movement_audit
    drop column location_type_id;

update temporary_absence_occurrence
set location = '{
  "description": "Location not found"
}'
where location is null;

alter table temporary_absence_occurrence
    alter column location set not null;