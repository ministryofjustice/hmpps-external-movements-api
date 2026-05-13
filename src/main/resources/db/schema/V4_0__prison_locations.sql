create table if not exists tap.prison_tap_locations
(
    prison_code varchar(6) not null,
    locations   jsonb      not null,
    version     int        not null,
    constraint pk_prison_tap_locations primary key (prison_code)
)
;

create table if not exists tap.prison_tap_locations_audit
(
    rev_id      bigint     not null references audit_revision (id),
    rev_type    smallint   not null,
    prison_code varchar(6) not null,
    locations   jsonb      not null,
    version     int        not null,
    constraint pk_prison_tap_locations_audit primary key (prison_code, rev_id)
)
;
