update tap.absence_type set description = 'Standard ROTL (release on temporary licence)' where code = 'SR';
update tap.absence_type set description = 'Restricted ROTL (release on temporary licence)' where code = 'RR';
update tap.absence_sub_type set description = 'CRL (childcare resettlement licence)' where code = 'CRL';
update tap.absence_reason set description = 'CRL (childcare resettlement licence)' where code = '3';
update tap.absence_sub_type set description = 'RDR (resettlement day release)' where code = 'RDR';
update tap.absence_sub_type set description = 'ROR (resettlement overnight release)' where code = 'ROR';
update tap.absence_sub_type set description = 'SPL (special purpose licence)' where code = 'SPL';
