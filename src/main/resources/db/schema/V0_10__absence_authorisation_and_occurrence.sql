drop table if exists temporary_absence_authorisation_audit;
drop table if exists temporary_absence_authorisation;

create table if not exists temporary_absence_authorisation
(
    id                  uuid        not null,
    version             int         not null,
    person_identifier   varchar(10) not null,
    prison_code         varchar(6)  not null,
    absence_type_id     bigint,
    absence_sub_type_id bigint,
    absence_reason_id   bigint,
    repeat              boolean     not null,
    status_id           bigint      not null,
    notes               text,
    application_date    date        not null,
    submitted_at        timestamp   not null,
    submitted_by        varchar(64) not null,
    approved_at         timestamp,
    approved_by         varchar(64),
    legacy_id           bigint,

    constraint pk_temporary_absence_authorisation primary key (id),
    constraint fk_temporary_absence_authorisation_absence_type_id foreign key (absence_type_id) references reference_data (id),
    constraint fk_temporary_absence_authorisation_absence_sub_type_id foreign key (absence_sub_type_id) references reference_data (id),
    constraint fk_temporary_absence_authorisation_absence_reason_id foreign key (absence_reason_id) references reference_data (id),
    constraint fk_temporary_absence_authorisation_status_id foreign key (status_id) references reference_data (id),
    constraint uq_temporary_absence_authorisation_legacy_id unique (legacy_id)
);

create table if not exists temporary_absence_authorisation_audit
(
    rev_id              bigint      not null references audit_revision (id),
    rev_type            smallint    not null,
    id                  uuid        not null,
    person_identifier   varchar(10) not null,
    prison_code         varchar(6)  not null,
    absence_type_id     bigint,
    absence_sub_type_id bigint,
    absence_reason_id   bigint,
    repeat              boolean     not null,
    status_id           bigint      not null,
    notes               text,
    application_date    date        not null,
    submitted_at        timestamp   not null,
    submitted_by        varchar(64) not null,
    approved_at         timestamp,
    approved_by         varchar(64),
    legacy_id           bigint,

    constraint pk_temporary_absence_authorisation_audit primary key (id, rev_id)
);

create table if not exists temporary_absence_occurrence
(
    id                uuid        not null,
    version           int         not null,
    person_identifier varchar(10) not null,
    authorisation_id  uuid        not null,
    release_at        timestamp   not null,
    return_by         timestamp   not null,
    location_type_id  bigint      not null,
    location_id       varchar(36) not null,
    accompanied_by_id bigint,
    transport_id      bigint,
    contact           text,
    notes             text,
    status_id         bigint      not null,
    added_at          timestamp   not null,
    added_by          varchar(64) not null,
    cancelled_at      timestamp,
    cancelled_by      varchar(64),
    legacy_id         bigint,

    constraint pk_temporary_absence_occurrence primary key (id),
    constraint fk_temporary_absence_occurrence_authorisation foreign key (authorisation_id) references temporary_absence_authorisation (id),
    constraint fk_temporary_absence_occurrence_location_type_id foreign key (location_type_id) references reference_data (id),
    constraint fk_temporary_absence_occurrence_accompanied_by_id foreign key (accompanied_by_id) references reference_data (id),
    constraint fk_temporary_absence_occurrence_transport_id foreign key (transport_id) references reference_data (id),
    constraint fk_temporary_absence_occurrence_status_id foreign key (status_id) references reference_data (id),
    constraint uq_temporary_absence_occurrence_legacy_id unique (legacy_id)
);

create index idx_temporary_absence_occurrence on temporary_absence_occurrence (person_identifier, release_at, return_by);

create table if not exists temporary_absence_occurrence_audit
(
    rev_id            bigint      not null references audit_revision (id),
    rev_type          smallint    not null,
    id                uuid        not null,
    person_identifier varchar(10) not null,
    authorisation_id  uuid        not null,
    release_at        timestamp   not null,
    return_by         timestamp   not null,
    location_type_id  bigint      not null,
    location_id       varchar(36) not null,
    accompanied_by_id bigint,
    transport_id      bigint,
    contact           text,
    notes             text,
    status_id         bigint      not null,
    added_at          timestamp   not null,
    added_by          varchar(64) not null,
    cancelled_at      timestamp,
    cancelled_by      varchar(64),
    legacy_id         bigint,

    constraint pk_temporary_absence_occurrence_audit primary key (id, rev_id)
);