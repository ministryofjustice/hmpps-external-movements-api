update temporary_absence_occurrence tao
set status_id = status.status_id
from temporary_absence_occurrence_status status
where status.occurrence_id = tao.id
;

drop view if exists tap_occurrence_status;
drop view if exists temporary_absence_occurrence_status;
drop function if exists get_occurrence_status_code(_occurrence_id uuid);

