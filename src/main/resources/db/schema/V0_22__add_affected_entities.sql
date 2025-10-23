alter table audit_revision
    add column affected_entities varchar[];

with audit_record as (select rev_id as rev_id, 'TemporaryAbsenceAuthorisation' as entity
                      from temporary_absence_authorisation_audit taa
                      union
                      select rev_id as revision_id, 'TemporaryAbsenceOccurrence' as entity
                      from temporary_absence_occurrence_audit
                      union
                      select rev_id as revision_id, 'TemporaryAbsenceMovement' as entity
                      from temporary_absence_movement_audit),
     grouped as (select rev_id as rev_id, array_agg(entity) as affected_entities from audit_record group by rev_id)
update audit_revision
set affected_entities = grouped.affected_entities
from grouped
where grouped.rev_id = audit_revision.id;

alter table audit_revision
    alter column affected_entities set not null;