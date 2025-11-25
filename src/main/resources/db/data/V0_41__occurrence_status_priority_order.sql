update reference_data
set sequence_number = 65
where domain = 'TAP_OCCURRENCE_STATUS'
  and code = 'PENDING'
;

update reference_data
set sequence_number = 10
where domain = 'TAP_OCCURRENCE_STATUS'
  and code = 'OVERDUE'
;

update reference_data
set sequence_number = 25
where domain = 'TAP_OCCURRENCE_STATUS'
  and code = 'SCHEDULED'
;

update reference_data
set sequence_number = 20
where domain = 'TAP_OCCURRENCE_STATUS'
  and code = 'IN_PROGRESS'
;

update reference_data
set sequence_number = 30
where domain = 'TAP_OCCURRENCE_STATUS'
  and code = 'SCHEDULED'
;

update reference_data
set sequence_number = 50
where domain = 'TAP_OCCURRENCE_STATUS'
  and code = 'CANCELLED'
;

update reference_data
set sequence_number = 70
where domain = 'TAP_OCCURRENCE_STATUS'
  and code = 'EXPIRED'
;

update reference_data
set sequence_number = 60
where domain = 'TAP_OCCURRENCE_STATUS'
  and code = 'PENDING'
;