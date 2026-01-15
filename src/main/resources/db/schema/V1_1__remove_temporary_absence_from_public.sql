alter table tap.movement
    add constraint fk_tap_movement_occurrence foreign key (occurrence_id) references tap.occurrence (id)
;

create or replace view tap.audited_authorisation as
select created_revision.username                                as created_by,
       created_revision.timestamp                               as created_at,
       updated_revision.username                                as updated_by,
       updated_revision.timestamp                               as updated_at,
       coalesce(updated_audit.rev_type, created_audit.rev_type) as rev_type,
       taa.*
from tap.authorisation taa
         join tap.authorisation_audit created_audit
              on created_audit.id = taa.id and created_audit.rev_type = 0
         join audit_revision created_revision on created_revision.id = created_audit.rev_id
         left join tap.authorisation_audit updated_audit
                   on updated_audit.id = taa.id and updated_audit.rev_type = 1 and updated_audit.rev_id =
                                                                                   (select max(audit.rev_id)
                                                                                    from tap.authorisation_audit audit
                                                                                    where audit.id = taa.id
                                                                                      and audit.rev_type = 1)
         left join audit_revision updated_revision on updated_revision.id = updated_audit.rev_id
;

create or replace view tap.audited_occurrence as
select created_revision.username                                as created_by,
       created_revision.timestamp                               as created_at,
       updated_revision.username                                as updated_by,
       updated_revision.timestamp                               as updated_at,
       coalesce(updated_audit.rev_type, created_audit.rev_type) as rev_type,
       tao.*
from tap.occurrence tao
         join tap.occurrence_audit created_audit
              on created_audit.id = tao.id and created_audit.rev_type = 0
         join audit_revision created_revision on created_revision.id = created_audit.rev_id
         left join tap.occurrence_audit updated_audit
                   on updated_audit.id = tao.id and updated_audit.rev_type = 1 and updated_audit.rev_id =
                                                                                   (select max(audit.rev_id)
                                                                                    from tap.occurrence_audit audit
                                                                                    where audit.id = tao.id
                                                                                      and audit.rev_type = 1)
         left join audit_revision updated_revision on updated_revision.id = updated_audit.rev_id
;

create or replace view tap.audited_movement as
select created_revision.username                                as created_by,
       created_revision.timestamp                               as created_at,
       updated_revision.username                                as updated_by,
       updated_revision.timestamp                               as updated_at,
       coalesce(updated_audit.rev_type, created_audit.rev_type) as rev_type,
       tam.*
from tap.movement tam
         join tap.movement_audit created_audit
              on created_audit.id = tam.id and created_audit.rev_type = 0
         join audit_revision created_revision on created_revision.id = created_audit.rev_id
         left join tap.movement_audit updated_audit
                   on updated_audit.id = tam.id and updated_audit.rev_type = 1 and updated_audit.rev_id =
                                                                                   (select max(audit.rev_id)
                                                                                    from tap.movement_audit audit
                                                                                    where audit.id = tam.id
                                                                                      and audit.rev_type = 1)
         left join audit_revision updated_revision on updated_revision.id = updated_audit.rev_id
;

drop view if exists audited_tap_movement;
drop view if exists audited_tap_occurrence;
drop view if exists audited_tap_authorisation;
drop table if exists temporary_absence_movement;
drop table if exists temporary_absence_occurrence;
drop table if exists temporary_absence_authorisation;
drop table if exists temporary_absence_movement_audit;
drop table if exists temporary_absence_occurrence_audit;
drop table if exists temporary_absence_authorisation_audit;
drop table if exists reference_data_link;
drop table if exists reference_data_domain_link;
drop table if exists reference_data;