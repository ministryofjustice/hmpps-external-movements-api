update tap.absence_sub_type
set groups = '{EXTERNAL_ACTIVITIES}'
where code in ('RDR', 'YTRC', 'YTRE')
;