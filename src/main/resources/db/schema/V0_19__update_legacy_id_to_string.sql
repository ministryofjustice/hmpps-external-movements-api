alter table temporary_absence_movement
    drop column legacy_id;

alter table temporary_absence_movement
    add column legacy_id varchar(32),
    add constraint uq_temporary_absence_movement_legacy_id unique (legacy_id);

alter table temporary_absence_movement_audit
    drop column legacy_id;

alter table temporary_absence_movement_audit
    add column legacy_id varchar(32);