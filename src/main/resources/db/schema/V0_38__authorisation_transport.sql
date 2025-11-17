alter table temporary_absence_authorisation
    add column if not exists transport_id bigint
;

alter table temporary_absence_authorisation_audit
    add column if not exists transport_id bigint
;

update temporary_absence_authorisation taa
set transport_id = tao.transport_id
from temporary_absence_occurrence tao
where tao.authorisation_id = taa.id
  and taa.transport_id is null
;

update temporary_absence_authorisation taa
set transport_id = (select rd.id from reference_data rd where rd.domain = 'TRANSPORT' and code = 'TNR')
from temporary_absence_occurrence tao
where taa.transport_id is null
;

update temporary_absence_authorisation_audit taaa
set transport_id = taa.transport_id
from temporary_absence_authorisation taa
where taa.id = taaa.id
;

alter table temporary_absence_authorisation
    alter column transport_id set not null
;

alter table temporary_absence_authorisation_audit
    alter column transport_id set not null
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
         left join audit_revision updated_revision on updated_revision.id = updated_audit.rev_id;