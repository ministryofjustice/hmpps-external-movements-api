update reference_data
set sequence_number = 35
where domain = 'TAP_AUTHORISATION_STATUS'
  and code = 'CANCELLED'
;

update reference_data
set sequence_number = 30
where domain = 'TAP_AUTHORISATION_STATUS'
  and code = 'DENIED'
;

update reference_data
set sequence_number = 40
where domain = 'TAP_AUTHORISATION_STATUS'
  and code = 'CANCELLED'
;