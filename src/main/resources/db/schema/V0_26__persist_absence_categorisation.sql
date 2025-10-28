alter table temporary_absence_authorisation
    add column absence_reason_category_id bigint,
    add column reason_path                jsonb,
    add constraint fk_temporary_absence_authorisation_absence_reason_category_id foreign key (absence_reason_category_id) references reference_data (id);

alter table temporary_absence_authorisation_audit
    add column absence_reason_category_id bigint,
    add column reason_path                jsonb;

update temporary_absence_authorisation
set reason_path = '{}'
where reason_path is null;

alter table temporary_absence_authorisation
    alter column reason_path set not null;

update temporary_absence_authorisation_audit
set reason_path = '{}'
where reason_path is null;

alter table temporary_absence_authorisation_audit
    alter column reason_path set not null;
