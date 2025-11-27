create index if not exists idx_tao_release_at_return_by on temporary_absence_occurrence (release_at, return_by)
;

alter table temporary_absence_authorisation_audit
    add column version int
;

alter table temporary_absence_occurrence_audit
    add column version int
;

alter table temporary_absence_movement_audit
    add column version int
;

with audit_version as (
    select taaa.id, row_number() over (partition by taaa.id order by taaa.rev_id) - 1 as version
    from temporary_absence_authorisation_audit taaa
)
update temporary_absence_authorisation_audit taaa
set version = av.version
from audit_version av
where av.id = taaa.id and taaa.version is null
;

with audit_version as (
    select taoa.id, row_number() over (partition by taoa.id order by taoa.rev_id) - 1 as version
    from temporary_absence_occurrence_audit taoa
)
update temporary_absence_occurrence_audit taoa
set version = av.version
from audit_version av
where av.id = taoa.id and taoa.version is null
;

with audit_version as (
    select tama.id, row_number() over (partition by tama.id order by tama.rev_id) - 1 as version
    from temporary_absence_movement_audit tama
)
update temporary_absence_movement_audit tama
set version = av.version
from audit_version av
where av.id = tama.id and tama.version is null
;

alter table temporary_absence_authorisation_audit
    alter column version set not null
;

alter table temporary_absence_occurrence_audit
    alter column version set not null
;

alter table temporary_absence_movement_audit
    alter column version set not null
;
