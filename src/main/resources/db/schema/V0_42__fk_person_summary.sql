alter table temporary_absence_authorisation
    add constraint fk_temporary_absence_authorisation_person_identifier foreign key (person_identifier) references person_summary (person_identifier)
;

alter table temporary_absence_movement
    add constraint fk_temporary_absence_movement_person_identifier foreign key (person_identifier) references person_summary (person_identifier)
;
