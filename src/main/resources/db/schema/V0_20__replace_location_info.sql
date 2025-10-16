alter table temporary_absence_authorisation
    add column if not exists schedule jsonb;

alter table temporary_absence_authorisation_audit
    add column if not exists schedule jsonb;

alter table temporary_absence_occurrence
    add column if not exists location           jsonb,
    add column if not exists schedule_reference jsonb,
    drop column if exists location_id,
    drop column if exists location_type_id;

alter table temporary_absence_occurrence_audit
    add column if not exists location           jsonb,
    add column if not exists schedule_reference jsonb,
    drop column if exists location_id,
    drop column if exists location_type_id;

alter table temporary_absence_occurrence
    rename column contact to contact_information;

alter table temporary_absence_occurrence_audit
    rename column contact to contact_information;

alter table temporary_absence_movement
    drop column if exists location_type_id;

alter table temporary_absence_movement_audit
    drop column if exists location_type_id;

update temporary_absence_occurrence
set location = '{
  "description": "Location not found"
}'
where location is null;

alter table temporary_absence_occurrence
    alter column location set not null;

update temporary_absence_occurrence_audit
set location = '{
  "description": "Location not found"
}'
where location is null;

alter table temporary_absence_occurrence_audit
    alter column location set not null;

alter table temporary_absence_movement
    drop column if exists location_id,
    drop column if exists location_type_id,
    drop column if exists location_description,
    drop column if exists location_premise,
    drop column if exists location_street,
    drop column if exists location_area,
    drop column if exists location_city,
    drop column if exists location_county,
    drop column if exists location_country,
    drop column if exists location_postcode,
    add column if not exists location jsonb;

alter table temporary_absence_movement_audit
    drop column if exists location_id,
    drop column if exists location_type_id,
    drop column if exists location_description,
    drop column if exists location_premise,
    drop column if exists location_street,
    drop column if exists location_area,
    drop column if exists location_city,
    drop column if exists location_county,
    drop column if exists location_country,
    drop column if exists location_postcode,
    add column if not exists location jsonb;

update temporary_absence_movement
set location = '{
  "description": "Location not found"
}'
where location is null;

alter table temporary_absence_movement
    alter column location set not null;

update temporary_absence_movement_audit
set location = '{
  "description": "Location not found"
}'
where location is null;

alter table temporary_absence_movement_audit
    alter column location set not null;