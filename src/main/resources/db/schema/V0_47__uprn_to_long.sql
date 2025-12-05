update temporary_absence_occurrence
set location = location || ('{"uprn":' || cast((location ->> 'uprn') as bigint) || '}')::jsonb
where location ->> 'uprn' is not null
;

update temporary_absence_movement
set location = location || ('{"uprn":' || cast((location ->> 'uprn') as bigint) || '}')::jsonb
where location ->> 'uprn' is not null
;