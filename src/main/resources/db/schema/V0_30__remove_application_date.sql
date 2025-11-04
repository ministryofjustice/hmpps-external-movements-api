alter table if exists temporary_absence_authorisation
    drop column if exists application_date;

alter table if exists temporary_absence_authorisation_audit
    drop column if exists application_date;

create or replace view tap_occurrence_status as
(
select taos.occurrence_id as id, rd.domain, rd.code, rd.description
from reference_data rd
         join temporary_absence_occurrence_status taos on taos.status_id = rd.id
         join temporary_absence_occurrence tao on tao.id = taos.occurrence_id
    );