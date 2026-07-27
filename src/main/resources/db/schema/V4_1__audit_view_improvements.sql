create index if not exists idx_hmpps_domain_event_audit_event_type_entity_id on hmpps_domain_event_audit (event_type, entity_id);

create index if not exists idx_authorisation_audit_id_rev_type_rev_id on tap.authorisation_audit (id, rev_type, rev_id desc);
create index if not exists idx_occurrence_audit_id_rev_type_rev_id on tap.occurrence_audit (id, rev_type, rev_id desc);
create index if not exists idx_movement_audit_id_rev_type_rev_id on tap.movement_audit (id, rev_type, rev_id desc);

create or replace view tap.audited_authorisation as
select created_revision.username                                as created_by,
       created_revision.timestamp                               as created_at,
       updated_revision.username                                as updated_by,
       updated_revision.timestamp                               as updated_at,
       coalesce(latest_update.rev_type, created_audit.rev_type) as rev_type,
       taa.*
from tap.authorisation taa
         join tap.authorisation_audit created_audit on created_audit.id = taa.id and created_audit.rev_type = 0
         join audit_revision created_revision on created_revision.id = created_audit.rev_id
         left join lateral (select update_audit.rev_id, update_audit.rev_type
                            from tap.authorisation_audit update_audit
                            where update_audit.id = taa.id
                              and update_audit.rev_type = 1
                            order by update_audit.rev_id desc
                            limit 1) latest_update on true
         left join audit_revision updated_revision on updated_revision.id = latest_update.rev_id
;

create or replace view tap.audited_occurrence as
select created_revision.username                                as created_by,
       created_revision.timestamp                               as created_at,
       updated_revision.username                                as updated_by,
       updated_revision.timestamp                               as updated_at,
       COALESCE(latest_update.rev_type, created_audit.rev_type) as rev_type,
       tao.*
from tap.occurrence tao
         join tap.occurrence_audit created_audit on created_audit.id = tao.id and created_audit.rev_type = 0
         join audit_revision created_revision on created_revision.id = created_audit.rev_id
         left join lateral (select update_audit.rev_id, update_audit.rev_type
                            from tap.occurrence_audit update_audit
                            where update_audit.id = tao.id
                              and update_audit.rev_type = 1
                            order by update_audit.rev_id desc
                            limit 1) latest_update on true
         left join audit_revision updated_revision on updated_revision.id = latest_update.rev_id;

create or replace view tap.audited_movement as
select created_revision.username                                as created_by,
       created_revision.timestamp                               as created_at,
       updated_revision.username                                as updated_by,
       updated_revision.timestamp                               as updated_at,
       coalesce(latest_update.rev_type, created_audit.rev_type) as rev_type,
       tam.*
from tap.movement tam
         join tap.movement_audit created_audit on created_audit.id = tam.id and created_audit.rev_type = 0
         join audit_revision created_revision on created_revision.id = created_audit.rev_id
         left join lateral (select update_audit.rev_id, update_audit.rev_type
                            from tap.movement_audit update_audit
                            where update_audit.id = tam.id
                              and update_audit.rev_type = 1
                            order by update_audit.rev_id desc
                            limit 1) latest_update on true
         left join audit_revision updated_revision on updated_revision.id = latest_update.rev_id
;