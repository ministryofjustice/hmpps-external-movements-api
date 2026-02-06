alter table tap.authorisation
    add column if not exists locations jsonb;

alter table tap.authorisation_audit
    add column if not exists locations jsonb;

update tap.authorisation ta
set locations = '[]'
where ta.locations is null;

update tap.authorisation_audit taa
set locations = '[]'
where taa.locations is null;

alter table tap.authorisation
    alter column locations set not null;

alter table tap.authorisation_audit
    alter column locations set not null;