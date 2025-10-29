update reference_data
set description = substr(description, 15)
where domain = 'ABSENCE_REASON' and description like 'Unpaid work - %';

update reference_data
set description = substr(description, 13)
where domain = 'ABSENCE_REASON' and description like 'Paid work - %';