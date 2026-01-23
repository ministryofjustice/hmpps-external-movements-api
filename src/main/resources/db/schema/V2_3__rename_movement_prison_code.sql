alter table tap.movement
    rename recorded_by_prison_code to direction_prison_code;

alter table tap.movement_audit
    rename recorded_by_prison_code to direction_prison_code;