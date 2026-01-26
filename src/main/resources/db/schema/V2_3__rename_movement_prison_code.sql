alter table tap.movement
    rename recorded_by_prison_code to prison_code;

alter table tap.movement_audit
    rename recorded_by_prison_code to prison_code;

alter view tap.audited_movement
    rename recorded_by_prison_code to prison_code;