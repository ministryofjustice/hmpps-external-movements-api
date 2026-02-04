alter table tap.authorisation
    add constraint fk_authorisation_person foreign key (person_identifier) references person_summary (person_identifier);
alter table tap.occurrence
    add constraint fk_occurrence_person foreign key (person_identifier) references person_summary (person_identifier);
alter table tap.movement
    add constraint fk_movement_person foreign key (person_identifier) references person_summary (person_identifier);