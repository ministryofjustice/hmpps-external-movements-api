alter table hmpps_domain_event
    add column if not exists version int
;

alter table hmpps_domain_event_audit
    add column if not exists version int
;

with de_version as (select id, row_number() over (partition by id order by rev_id desc) as version
                    from hmpps_domain_event_audit)
update hmpps_domain_event_audit dea
set version = de_version.version
from de_version
where dea.id = de_version.id
  and dea.version is null
;

update hmpps_domain_event de
set version = (select max(version) from hmpps_domain_event_audit dea where dea.id = de.id)
where de.version is null
;

alter table hmpps_domain_event
    alter column version set not null;

alter table hmpps_domain_event_audit
    alter column version set not null;