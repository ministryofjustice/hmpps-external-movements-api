update reference_data
set description = 'Unaccompanied'
where domain = 'ACCOMPANIED_BY'
  and code = 'U';

update reference_data
set description = 'Transport not required'
where domain = 'TRANSPORT'
  and code = 'TRN';