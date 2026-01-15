update tap.absence_reason set description = 'RDR (resettlement day release)' where code = 'YRDR';
update tap.absence_reason set description = 'ROR (resettlement overnight release)' where code = 'RO';
update tap.absence_reason set description = 'Police production' where code = 'PC';
update tap.absence_reason set description = 'Attend religious service' where code = 'C9';
update tap.absence_reason set description = 'Transfer via temporary release' where code = 'TRNTAP';
update tap.absence_reason set description = 'Unescorted court appearance' where code = 'F4';