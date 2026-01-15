alter table hmpps_domain_event
    add column if not exists entity_id uuid
;

alter table hmpps_domain_event_audit
    add column if not exists entity_id uuid
;

update hmpps_domain_event
set entity_id = ((event ->> 'additionalInformation')::jsonb ->> 'id')::uuid
where entity_id is null
;

update hmpps_domain_event_audit dea
set entity_id = de.entity_id
from hmpps_domain_event de
where dea.id = de.id
;
