alter table temporary_absence_authorisation
    add column accompanied_by_id bigint
        constraint fk_temporary_absence_authorisation_accompanied_by_id references reference_data (id)
;

alter table temporary_absence_authorisation_audit
    add column accompanied_by_id bigint
;

update temporary_absence_authorisation taa
set accompanied_by_id = (select accompanied_by_id
                         from temporary_absence_occurrence tao
                         where tao.authorisation_id = taa.id
                         limit 1)
where taa.accompanied_by_id is null
;

update temporary_absence_authorisation_audit taaa
set accompanied_by_id = (select accompanied_by_id
                         from temporary_absence_authorisation taa
                         where taa.id = taaa.id)
where taaa.accompanied_by_id is null
;

alter table temporary_absence_authorisation
    alter column accompanied_by_id set not null
;

alter table temporary_absence_authorisation_audit
    alter column accompanied_by_id set not null
;

alter table temporary_absence_occurrence
    add column absence_type_id            bigint
        constraint fk_temporary_absence_occurrence_absence_type_id references reference_data (id),
    add column absence_sub_type_id        bigint
        constraint fk_temporary_absence_occurrence_absence_sub_type_id references reference_data (id),
    add column absence_reason_category_id bigint
        constraint fk_temporary_absence_occurrence_absence_reason_category_id references reference_data (id),
    add column absence_reason_id          bigint
        constraint fk_temporary_absence_occurrence_absence_reason_id references reference_data (id),
    add column reason_path                jsonb
;

alter table temporary_absence_occurrence_audit
    add column absence_type_id            bigint,
    add column absence_sub_type_id        bigint,
    add column absence_reason_category_id bigint,
    add column absence_reason_id          bigint,
    add column reason_path                jsonb
;

update temporary_absence_occurrence tao
set absence_type_id            = taa.absence_type_id,
    absence_sub_type_id        = taa.absence_sub_type_id,
    absence_reason_category_id = taa.absence_reason_category_id,
    absence_reason_id          = taa.absence_reason_id,
    reason_path                = taa.reason_path
from temporary_absence_authorisation taa
where taa.id = tao.authorisation_id
;

update temporary_absence_occurrence_audit taoa
set absence_type_id            = tao.absence_type_id,
    absence_sub_type_id        = tao.absence_sub_type_id,
    absence_reason_category_id = tao.absence_reason_category_id,
    absence_reason_id          = tao.absence_reason_id,
    reason_path                = tao.reason_path
from temporary_absence_occurrence tao
where tao.id = taoa.id
;

alter table temporary_absence_occurrence
    alter column reason_path set not null
;

alter table temporary_absence_occurrence_audit
    alter column reason_path set not null
;

create or replace view audited_tap_authorisation as
select created_revision.username                                as created_by,
       created_revision.timestamp                               as created_at,
       updated_revision.username                                as updated_by,
       updated_revision.timestamp                               as updated_at,
       coalesce(updated_audit.rev_type, created_audit.rev_type) as rev_type,
       taa.*
from temporary_absence_authorisation taa
         join temporary_absence_authorisation_audit created_audit
              on created_audit.id = taa.id and created_audit.rev_type = 0
         join audit_revision created_revision on created_revision.id = created_audit.rev_id
         left join temporary_absence_authorisation_audit updated_audit
                   on updated_audit.id = taa.id and updated_audit.rev_type = 1 and updated_audit.rev_id =
                                                                                   (select max(audit.rev_id)
                                                                                    from temporary_absence_authorisation_audit audit
                                                                                    where audit.id = taa.id
                                                                                      and audit.rev_type = 1)
         left join audit_revision updated_revision on updated_revision.id = updated_audit.rev_id
;

create or replace view audited_tap_occurrence as
select created_revision.username                                as created_by,
       created_revision.timestamp                               as created_at,
       updated_revision.username                                as updated_by,
       updated_revision.timestamp                               as updated_at,
       coalesce(updated_audit.rev_type, created_audit.rev_type) as rev_type,
       tao.*
from temporary_absence_occurrence tao
         join temporary_absence_occurrence_audit created_audit
              on created_audit.id = tao.id and created_audit.rev_type = 0
         join audit_revision created_revision on created_revision.id = created_audit.rev_id
         left join temporary_absence_occurrence_audit updated_audit
                   on updated_audit.id = tao.id and updated_audit.rev_type = 1 and updated_audit.rev_id =
                                                                                   (select max(audit.rev_id)
                                                                                    from temporary_absence_occurrence_audit audit
                                                                                    where audit.id = tao.id
                                                                                      and audit.rev_type = 1)
         left join audit_revision updated_revision on updated_revision.id = updated_audit.rev_id
;