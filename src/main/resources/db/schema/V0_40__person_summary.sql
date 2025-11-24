create table if not exists person_summary
(
    person_identifier varchar(10) primary key not null,
    first_name        varchar(64)             not null,
    last_name         varchar(64)             not null,
    date_of_birth     date                    not null,
    cell_location     varchar(64),
    version           int                     not null
)
;

insert into person_summary(person_identifier, first_name, last_name, date_of_birth, cell_location, version)
select person_identifier, '', '', current_date, null, 0
from temporary_absence_authorisation
union
select person_identifier, '', '', current_date, null, 0
from temporary_absence_movement
on conflict do nothing
;
