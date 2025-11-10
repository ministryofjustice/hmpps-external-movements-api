alter table temporary_absence_occurrence
    add column if not exists status_id bigint
        constraint fk_temporary_absence_occurrence_status_id references reference_data (id)
;

alter table temporary_absence_occurrence_audit
    add column if not exists status_id bigint
;

update temporary_absence_occurrence tao
set status_id = status.status_id
from temporary_absence_occurrence_status status
where status.occurrence_id = tao.id
;

update temporary_absence_occurrence_audit taoa
set status_id = tao.status_id
from temporary_absence_occurrence tao
where tao.id = taoa.id
;

alter table temporary_absence_occurrence
    alter column status_id set not null;


alter table temporary_absence_occurrence_audit
    alter column status_id set not null;

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