create schema if not exists tap
;

create table if not exists tap.reference_data
(
    id              uuid         not null default uuidv7(),
    code            varchar(16)  not null,
    description     varchar(255) not null,
    sequence_number int          not null,
    active          boolean      not null,
    -- to be dropped after transferring data
    legacy_id       bigint       not null
)
;

create table if not exists tap.absence_type
(
    next_domain varchar(32),
    constraint pk_absence_type primary key (id),
    constraint uq_absence_type_code unique (code),
    constraint uq_absence_type_sequence_number unique (sequence_number)
) inherits (tap.reference_data)
;

create table if not exists tap.absence_sub_type
(
    next_domain varchar(32),
    hint_text   varchar(255),
    constraint pk_absence_sub_type primary key (id),
    constraint uq_absence_sub_type_code unique (code),
    constraint uq_absence_sub_type_sequence_number unique (sequence_number)
) inherits (tap.reference_data)
;

create table if not exists tap.absence_reason_category
(
    next_domain varchar(32),
    constraint pk_absence_reason_category primary key (id),
    constraint uq_absence_reason_category_code unique (code),
    constraint uq_absence_reason_category_sequence_number unique (sequence_number)
) inherits (tap.reference_data)
;

create table if not exists tap.absence_reason
(
    constraint pk_absence_reason primary key (id),
    constraint uq_absence_reason_code unique (code),
    constraint uq_absence_reason_sequence_number unique (sequence_number)
) inherits (tap.reference_data)
;

create table if not exists tap.accompanied_by
(
    constraint pk_accompanied_by primary key (id),
    constraint uq_accompanied_by_code unique (code),
    constraint uq_accompanied_by_sequence_number unique (sequence_number)
) inherits (tap.reference_data)
;

create table if not exists tap.transport
(
    constraint pk_transport primary key (id),
    constraint uq_transport_code unique (code),
    constraint uq_transport_sequence_number unique (sequence_number)
) inherits (tap.reference_data)
;

create table if not exists tap.authorisation_status
(
    constraint pk_authorisation_status primary key (id),
    constraint uq_authorisation_status_code unique (code),
    constraint uq_authorisation_status_sequence_number unique (sequence_number)
) inherits (tap.reference_data)
;

create table if not exists tap.occurrence_status
(
    constraint pk_occurrence_status primary key (id),
    constraint uq_occurrence_status_code unique (code),
    constraint uq_occurrence_status_sequence_number unique (sequence_number)
) inherits (tap.reference_data)
;

insert into tap.absence_type(code, description, sequence_number, active, legacy_id, next_domain)
select code, description, sequence_number, active, rd.id, rddl.domain
from public.reference_data rd
         left join public.reference_data_domain_link rddl on rddl.id = rd.id
where rd.domain = 'ABSENCE_TYPE'
on conflict do nothing
;

insert into tap.absence_sub_type(code, description, sequence_number, active, hint_text, legacy_id, next_domain)
select code, description, sequence_number, active, hint_text, rd.id, rddl.domain
from public.reference_data rd
         left join public.reference_data_domain_link rddl on rddl.id = rd.id
where rd.domain = 'ABSENCE_SUB_TYPE'
on conflict do nothing
;

insert into tap.absence_reason_category(code, description, sequence_number, active, legacy_id, next_domain)
select code, description, sequence_number, active, rd.id, rddl.domain
from public.reference_data rd
         left join public.reference_data_domain_link rddl on rddl.id = rd.id
where rd.domain = 'ABSENCE_REASON_CATEGORY'
on conflict do nothing
;

insert into tap.absence_reason(code, description, sequence_number, active, legacy_id)
select code, description, sequence_number, active, rd.id
from public.reference_data rd
where rd.domain = 'ABSENCE_REASON'
on conflict do nothing
;

insert into tap.accompanied_by(code, description, sequence_number, active, legacy_id)
select code, description, sequence_number, active, rd.id
from public.reference_data rd
where rd.domain = 'ACCOMPANIED_BY'
on conflict do nothing
;

insert into tap.transport(code, description, sequence_number, active, legacy_id)
select code, description, sequence_number, active, rd.id
from public.reference_data rd
where rd.domain = 'TRANSPORT'
on conflict do nothing
;

insert into tap.authorisation_status(code, description, sequence_number, active, legacy_id)
select code, description, sequence_number, active, rd.id
from public.reference_data rd
where rd.domain = 'TAP_AUTHORISATION_STATUS'
on conflict do nothing
;

insert into tap.occurrence_status(code, description, sequence_number, active, legacy_id)
select code, description, sequence_number, active, rd.id
from public.reference_data rd
where rd.domain = 'TAP_OCCURRENCE_STATUS'
on conflict do nothing
;

create table if not exists tap.authorisation
(
    id                         uuid        not null,
    version                    int         not null,
    person_identifier          varchar(10) not null,
    prison_code                varchar(6)  not null,
    status_id                  uuid        not null,
    absence_type_id            uuid,
    absence_sub_type_id        uuid,
    absence_reason_category_id uuid,
    absence_reason_id          uuid        not null,
    accompanied_by_id          uuid        not null,
    transport_id               uuid        not null,
    repeat                     boolean     not null,
    comments                   text,
    start                      date        not null,
    "end"                      date        not null,
    reason_path                jsonb       not null,
    schedule                   jsonb,
    legacy_id                  bigint,

    constraint pk_tap_authorisation primary key (id),
    constraint fk_tap_authorisation_status foreign key (status_id) references tap.authorisation_status (id),
    constraint fk_tap_authorisation_absence_type foreign key (absence_type_id) references tap.absence_type (id),
    constraint fk_tap_authorisation_absence_sub_type foreign key (absence_sub_type_id) references tap.absence_sub_type (id),
    constraint fk_tap_authorisation_absence_reason_category foreign key (absence_reason_category_id) references tap.absence_reason_category (id),
    constraint fk_tap_authorisation_absence_reason foreign key (absence_reason_id) references tap.absence_reason (id),
    constraint fk_tap_authorisation_accompanied_by foreign key (accompanied_by_id) references tap.accompanied_by (id),
    constraint fk_tap_authorisation_transport foreign key (transport_id) references tap.transport (id),
    constraint uq_tap_authorisation_legacy_id unique (legacy_id)
)
;

create index if not exists idx_tap_authorisation_prison_person_start_end on tap.authorisation (prison_code, person_identifier, start, "end");
create index if not exists idx_tap_authorisation_status_end on tap.authorisation (status_id, "end");

create table if not exists tap.authorisation_audit
(
    rev_id                     bigint      not null references audit_revision (id),
    rev_type                   smallint    not null,
    id                         uuid        not null,
    version                    int         not null,
    person_identifier          varchar(10) not null,
    prison_code                varchar(6)  not null,
    status_id                  uuid        not null,
    absence_type_id            uuid,
    absence_sub_type_id        uuid,
    absence_reason_category_id uuid,
    absence_reason_id          uuid        not null,
    accompanied_by_id          uuid        not null,
    transport_id               uuid        not null,
    start                      date        not null,
    "end"                      date        not null,
    repeat                     boolean     not null,
    comments                   text,
    reason_path                jsonb       not null,
    schedule                   jsonb,
    legacy_id                  bigint,

    constraint pk_tap_authorisation_audit primary key (id, rev_id)
)
;

create table if not exists tap.occurrence
(
    id                         uuid      not null,
    version                    int       not null,
    authorisation_id           uuid      not null,
    status_id                  uuid      not null,
    absence_type_id            uuid,
    absence_sub_type_id        uuid,
    absence_reason_category_id uuid,
    absence_reason_id          uuid      not null,
    accompanied_by_id          uuid      not null,
    transport_id               uuid      not null,
    start                      timestamp not null,
    "end"                      timestamp not null,
    location                   jsonb     not null,
    contact_information        text,
    comments                   text,
    reason_path                jsonb     not null,
    schedule_reference         jsonb,
    legacy_id                  bigint,

    constraint pk_tap_occurrence primary key (id),
    constraint fk_tap_occurrence_status foreign key (status_id) references tap.occurrence_status (id),
    constraint fk_tap_occurrence_absence_type foreign key (absence_type_id) references tap.absence_type (id),
    constraint fk_tap_occurrence_absence_sub_type foreign key (absence_sub_type_id) references tap.absence_sub_type (id),
    constraint fk_tap_occurrence_absence_reason_category foreign key (absence_reason_category_id) references tap.absence_reason_category (id),
    constraint fk_tap_occurrence_absence_reason foreign key (absence_reason_id) references tap.absence_reason (id),
    constraint fk_tap_occurrence_accompanied_by foreign key (accompanied_by_id) references tap.accompanied_by (id),
    constraint fk_tap_occurrence_transport foreign key (transport_id) references tap.transport (id),
    constraint uq_tap_occurrence_legacy_id unique (legacy_id)
)
;

create index if not exists idx_tap_occurrence_authorisation_id on tap.occurrence (authorisation_id);
create index if not exists idx_tap_occurrence_start_end on tap.occurrence (start, "end");
create index if not exists idx_tap_occurrence_status_id_end on tap.occurrence (status_id, "end");

create table if not exists tap.occurrence_audit
(
    rev_id                     bigint    not null references audit_revision (id),
    rev_type                   smallint  not null,
    id                         uuid      not null,
    version                    int       not null,
    authorisation_id           uuid      not null,
    status_id                  uuid      not null,
    absence_type_id            uuid,
    absence_sub_type_id        uuid,
    absence_reason_category_id uuid,
    absence_reason_id          uuid      not null,
    accompanied_by_id          uuid      not null,
    transport_id               uuid      not null,
    start                      timestamp not null,
    "end"                      timestamp not null,
    location                   jsonb     not null,
    contact_information        text,
    comments                   text,
    reason_path                jsonb     not null,
    schedule_reference         jsonb,
    legacy_id                  bigint,

    constraint pk_tap_occurrence_audit primary key (id, rev_id)
)
;

create table if not exists tap.movement
(
    id                      uuid        not null,
    version                 int         not null,
    person_identifier       varchar(10) not null,
    occurrence_id           uuid,
    occurred_at             timestamp   not null,
    direction               varchar(3)  not null,
    absence_reason_id       uuid        not null,
    accompanied_by_id       uuid        not null,
    accompanied_by_comments text,
    location                jsonb       not null,
    comments                text,
    recorded_by_prison_code varchar(6)  not null,
    legacy_id               varchar(32),

    constraint pk_tap_movement primary key (id),
    constraint fk_tap_movement_absence_reason foreign key (absence_reason_id) references tap.absence_reason (id),
    constraint fk_tap_movement_accompanied_by foreign key (accompanied_by_id) references tap.accompanied_by (id),
    constraint uq_tap_movement_legacy_id unique (legacy_id)
)
;

create index if not exists idx_tap_movement_occurrence on tap.movement (occurrence_id) where occurrence_id is not null;
create index if not exists idx_tap_movement_person_occurred on tap.movement (person_identifier, occurred_at);

create table if not exists tap.movement_audit
(
    rev_id                  bigint      not null references audit_revision (id),
    rev_type                smallint    not null,
    id                      uuid        not null,
    version                 int         not null,
    person_identifier       varchar(10) not null,
    occurrence_id           uuid,
    occurred_at             timestamp   not null,
    direction               varchar(3)  not null,
    absence_reason_id       uuid        not null,
    accompanied_by_id       uuid        not null,
    accompanied_by_comments text,
    location                jsonb       not null,
    comments                text,
    recorded_by_prison_code varchar(6)  not null,
    legacy_id               varchar(32),

    constraint pk_tap_movement_audit primary key (id, rev_id)
)
;

create table if not exists tap.absence_categorisation_domain_link
(
    id     uuid        not null,
    domain varchar(32) not null,
    constraint fk_tap_absence_categorisation_domain_link_domain foreign key (domain) references reference_data_domain (code)
)
;

insert into tap.absence_categorisation_domain_link(id, domain)
select (select rd.id from tap.reference_data rd where rd.legacy_id = rddl.id), rddl.domain
from reference_data_domain_link rddl
on conflict do nothing
;

create table if not exists tap.absence_categorisation_link
(
    id              uuid        not null default uuidv7(),
    domain_1        varchar(32) not null,
    id_1            uuid        not null,
    domain_2        varchar(32) not null,
    id_2            uuid        not null,
    sequence_number int         not null,
    constraint pk_absence_categorisation_link primary key (id),
    constraint uq_reference_data_link_sequence_number unique (id_1, sequence_number)
)
;

insert into tap.absence_categorisation_link(domain_1, id_1, domain_2, id_2, sequence_number)
select (select rd1.domain from reference_data rd1 where rd1.id = rdl.reference_data_id_1),
       (select ac1.id from tap.reference_data ac1 where ac1.legacy_id = rdl.reference_data_id_1),
       (select rd2.domain from reference_data rd2 where rd2.id = rdl.reference_data_id_2),
       (select ac2.id from tap.reference_data ac2 where ac2.legacy_id = rdl.reference_data_id_2),
       sequence_number
from reference_data_link rdl
on conflict do nothing
;

create or replace view tap.audited_authorisation as
select created_revision.username                                as created_by,
       created_revision.timestamp                               as created_at,
       updated_revision.username                                as updated_by,
       updated_revision.timestamp                               as updated_at,
       coalesce(updated_audit.rev_type, created_audit.rev_type) as rev_type,
       taa.*
from tap.authorisation taa
         join tap.authorisation_audit created_audit
              on created_audit.id = taa.id and created_audit.rev_type = 0
         join audit_revision created_revision on created_revision.id = created_audit.rev_id
         left join tap.authorisation_audit updated_audit
                   on updated_audit.id = taa.id and updated_audit.rev_type = 1 and updated_audit.rev_id =
                                                                                   (select max(audit.rev_id)
                                                                                    from temporary_absence_authorisation_audit audit
                                                                                    where audit.id = taa.id
                                                                                      and audit.rev_type = 1)
         left join audit_revision updated_revision on updated_revision.id = updated_audit.rev_id
;

create or replace view tap.audited_occurrence as
select created_revision.username                                as created_by,
       created_revision.timestamp                               as created_at,
       updated_revision.username                                as updated_by,
       updated_revision.timestamp                               as updated_at,
       coalesce(updated_audit.rev_type, created_audit.rev_type) as rev_type,
       tao.*
from tap.occurrence tao
         join tap.occurrence_audit created_audit
              on created_audit.id = tao.id and created_audit.rev_type = 0
         join audit_revision created_revision on created_revision.id = created_audit.rev_id
         left join tap.occurrence_audit updated_audit
                   on updated_audit.id = tao.id and updated_audit.rev_type = 1 and updated_audit.rev_id =
                                                                                   (select max(audit.rev_id)
                                                                                    from temporary_absence_occurrence_audit audit
                                                                                    where audit.id = tao.id
                                                                                      and audit.rev_type = 1)
         left join audit_revision updated_revision on updated_revision.id = updated_audit.rev_id
;

create or replace view tap.audited_movement as
select created_revision.username                                as created_by,
       created_revision.timestamp                               as created_at,
       updated_revision.username                                as updated_by,
       updated_revision.timestamp                               as updated_at,
       coalesce(updated_audit.rev_type, created_audit.rev_type) as rev_type,
       tam.*
from tap.movement tam
         join tap.movement_audit created_audit
              on created_audit.id = tam.id and created_audit.rev_type = 0
         join audit_revision created_revision on created_revision.id = created_audit.rev_id
         left join tap.movement_audit updated_audit
                   on updated_audit.id = tam.id and updated_audit.rev_type = 1 and updated_audit.rev_id =
                                                                                   (select max(audit.rev_id)
                                                                                    from temporary_absence_movement_audit audit
                                                                                    where audit.id = tam.id
                                                                                      and audit.rev_type = 1)
         left join audit_revision updated_revision on updated_revision.id = updated_audit.rev_id
;