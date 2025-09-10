create table if not exists temporary_absence_series
(
    id                  uuid        not null,
    person_identifier   varchar(10) not null,
    prison_code         varchar(6)  not null,
    absence_type_id     bigint,
    absence_sub_type_id bigint,
    absence_reason_id   bigint,
    repeat              boolean     not null,
    release_at          timestamp   not null,
    return_by           timestamp   not null,
    location_type_id    bigint      not null,
    location_id         varchar(36),
    accompanied         boolean     not null,
    accompanied_by_id   bigint,
    transport_id        bigint,
    status_id           bigint      not null,
    notes               text,
    submitted_at        timestamp   not null,
    legacy_id           bigint,
    to_agency_code      varchar(6),

    constraint pk_temporary_absence_series primary key (id),
    constraint fk_temporary_absence_series_absence_type_id foreign key (absence_type_id) references reference_data (id),
    constraint fk_temporary_absence_series_absence_sub_type_id foreign key (absence_sub_type_id) references reference_data (id),
    constraint fk_temporary_absence_series_absence_reason_id foreign key (absence_reason_id) references reference_data (id),
    constraint fk_temporary_absence_series_accompanied_by_id foreign key (accompanied_by_id) references reference_data (id),
    constraint fk_temporary_absence_series_transport_id foreign key (transport_id) references reference_data (id),
    constraint fk_temporary_absence_series_status_id foreign key (status_id) references reference_data (id),
    constraint fk_temporary_absence_series_location_type_id foreign key (location_type_id) references reference_data (id),
    constraint uq_temporary_absence_series_legacy_id unique (legacy_id)
);

create index if not exists idx_temporary_absence_series_person_identifier on temporary_absence_series (person_identifier, release_at, return_by);

create table audit_revision
(
    id          bigserial   not null,
    timestamp   timestamp   not null,
    username    varchar(64) not null,
    caseload_id varchar(10),
    source      varchar(6)  not null,
    constraint pk_audit_revision primary key (id),
    constraint ch_audit_revision_source check (source in ('DPS', 'NOMIS'))
);

create table if not exists temporary_absence_series_audit
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
    release_at          timestamp   not null,
    return_by           timestamp   not null,
    location_type_id    bigint      not null,
    location_id         varchar(36),
    accompanied         boolean     not null,
    accompanied_by_id   bigint,
    transport_id        bigint,
    status_id           bigint      not null,
    notes               text,
    submitted_at        timestamp   not null,
    legacy_id           bigint,
    to_agency_code      varchar(6),
    constraint pk_temporary_absence_series_audit primary key (id, rev_id)
);