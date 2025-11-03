alter table temporary_absence_movement
    add constraint fk_temporary_absence_movement_occurrence_id foreign key (occurrence_id) references temporary_absence_occurrence (id);

create table if not exists tap_occurrence_action
(
    id            uuid not null,
    occurrence_id uuid not null,
    type          text not null,
    reason        text,
    constraint pk_tap_occurrence_action primary key (id),
    constraint fk_tap_occurrence_action_occurrence_id foreign key (occurrence_id) references temporary_absence_occurrence (id)
);

create table if not exists tap_occurrence_action_audit
(
    rev_id        bigint   not null references audit_revision (id),
    rev_type      smallint not null,
    id            uuid     not null,
    occurrence_id uuid     not null,
    type          text     not null,
    reason        text,
    constraint pk_tap_occurrence_action_audit primary key (id, rev_id)
);