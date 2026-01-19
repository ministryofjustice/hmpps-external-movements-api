drop table if exists tap.absence_categorisation_domain_link;
drop view if exists tap.audited_movement;
drop view if exists tap.audited_occurrence;
drop table if exists tap.movement_audit;
drop table if exists tap.occurrence_audit;
drop table if exists tap.movement;
drop table if exists tap.occurrence;

create table if not exists tap.occurrence
(
    id                         uuid        not null,
    version                    int         not null,
    person_identifier          varchar(10) not null,
    prison_code                varchar(6)  not null,
    authorisation_id           uuid        not null,
    status_id                  uuid        not null,
    absence_type_id            uuid,
    absence_sub_type_id        uuid,
    absence_reason_category_id uuid,
    absence_reason_id          uuid        not null,
    accompanied_by_id          uuid        not null,
    transport_id               uuid        not null,
    start                      timestamp   not null,
    "end"                      timestamp   not null,
    location                   jsonb       not null,
    contact_information        text,
    comments                   text,
    reason_path                jsonb       not null,
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
create index if not exists idx_tap_occurrence_prison_person_start_end on tap.authorisation (prison_code, person_identifier, start, "end");
create index if not exists idx_tap_occurrence_status_id_end on tap.occurrence (status_id, "end");

create table if not exists tap.occurrence_audit
(
    rev_id                     bigint      not null references audit_revision (id),
    rev_type                   smallint    not null,
    id                         uuid        not null,
    version                    int         not null,
    person_identifier          varchar(10) not null,
    prison_code                varchar(6)  not null,
    authorisation_id           uuid        not null,
    status_id                  uuid        not null,
    absence_type_id            uuid,
    absence_sub_type_id        uuid,
    absence_reason_category_id uuid,
    absence_reason_id          uuid        not null,
    accompanied_by_id          uuid        not null,
    transport_id               uuid        not null,
    start                      timestamp   not null,
    "end"                      timestamp   not null,
    location                   jsonb       not null,
    contact_information        text,
    comments                   text,
    reason_path                jsonb       not null,
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
    constraint fk_tap_movement_occurrence foreign key (occurrence_id) references tap.occurrence (id),
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
                                                                                    from tap.occurrence_audit audit
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
                                                                                    from tap.movement_audit audit
                                                                                    where audit.id = tam.id
                                                                                      and audit.rev_type = 1)
         left join audit_revision updated_revision on updated_revision.id = updated_audit.rev_id
;