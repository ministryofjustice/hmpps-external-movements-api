drop table tap_occurrence_action;
drop table tap_occurrence_action_audit;
update audit_revision
set affected_entities = array_remove(affected_entities, 'TapOccurrenceAction')
where 'TapOccurrenceAction' = any (affected_entities);

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
         left join audit_revision updated_revision on updated_revision.id = updated_audit.rev_id;

create or replace view audited_tap_movement as
select created_revision.username                                as created_by,
       created_revision.timestamp                               as created_at,
       updated_revision.username                                as updated_by,
       updated_revision.timestamp                               as updated_at,
       coalesce(updated_audit.rev_type, created_audit.rev_type) as rev_type,
       tam.*
from temporary_absence_movement tam
         join temporary_absence_movement_audit created_audit
              on created_audit.id = tam.id and created_audit.rev_type = 0
         join audit_revision created_revision on created_revision.id = created_audit.rev_id
         left join temporary_absence_movement_audit updated_audit
                   on updated_audit.id = tam.id and updated_audit.rev_type = 1 and updated_audit.rev_id =
                                                                                   (select max(audit.rev_id)
                                                                                    from temporary_absence_movement_audit audit
                                                                                    where audit.id = tam.id
                                                                                      and audit.rev_type = 1)
         left join audit_revision updated_revision on updated_revision.id = updated_audit.rev_id;