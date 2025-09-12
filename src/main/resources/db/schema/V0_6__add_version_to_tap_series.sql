alter table temporary_absence_series
    add column if not exists version int;

update temporary_absence_series
set version = 0
where version is null;

alter table temporary_absence_series
    alter column version set not null;