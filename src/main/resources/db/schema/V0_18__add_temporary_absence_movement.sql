create table if not exists temporary_absence_movement
(
    id                      uuid        not null,
    version                 int         not null,
    person_identifier       varchar(10) not null,
    occurrence_id           uuid,
    occurred_at             timestamp   not null,
    direction               varchar(3)  not null,
    absence_reason_id       bigint      not null,
    accompanied_by_id       bigint      not null,
    accompanied_by_notes    text,
    notes                   text,
    recorded_at             timestamp   not null,
    recorded_by             varchar(64) not null,
    recorded_by_prison_code varchar(6)  not null,

    location_id             varchar(36),
    location_type_id        bigint      not null,
    location_description    text,
    location_premise        text,
    location_street         text,
    location_area           text,
    location_city           text,
    location_county         text,
    location_country        text,
    location_postcode       text,

    legacy_id               bigint,

    constraint pk_temporary_absence_movement primary key (id),
    constraint fk_temporary_absence_movement_absence_reason_id foreign key (absence_reason_id) references reference_data (id),
    constraint fk_temporary_absence_movement_accompanied_by_id foreign key (accompanied_by_id) references reference_data (id),
    constraint fk_temporary_absence_occurrence_location_type_id foreign key (location_type_id) references reference_data (id),
    constraint uq_temporary_absence_movement_legacy_id unique (legacy_id)
);

create index idx_temporary_absence_movement on temporary_absence_movement (person_identifier, occurred_at);

create table if not exists temporary_absence_movement_audit
(
    rev_id                  bigint      not null references audit_revision (id),
    rev_type                smallint    not null,
    id                      uuid        not null,
    person_identifier       varchar(10) not null,
    occurrence_id           uuid,
    occurred_at             timestamp   not null,
    direction               varchar(3)  not null,
    absence_reason_id       bigint      not null,
    accompanied_by_id       bigint      not null,
    accompanied_by_notes    text,
    notes                   text,
    recorded_at             timestamp   not null,
    recorded_by             varchar(64) not null,
    recorded_by_prison_code varchar(6)  not null,

    location_id             varchar(36),
    location_type_id        bigint      not null,
    location_description    text,
    location_premise        text,
    location_street         text,
    location_area           text,
    location_city           text,
    location_county         text,
    location_country        text,
    location_postcode       text,

    legacy_id               bigint,

    constraint pk_temporary_absence_movement_audit primary key (id, rev_id)
);