create table if not exists hmpps_domain_event
(
    id         uuid    not null,
    event_type text    not null,
    event      jsonb   not null,
    published  boolean not null,
    constraint pk_hmpps_domain_event primary key (id)
);

create index if not exists hmpps_domain_event_unpublished on hmpps_domain_event (id) where published = false;

create table if not exists hmpps_domain_event_audit
(
    rev_id     bigint   not null references audit_revision (id),
    rev_type   smallint not null,
    id         uuid     not null,
    event_type text     not null,
    event      jsonb    not null,
    published  boolean  not null,
    constraint pk_hmpps_domain_event_audit primary key (id, rev_id)
);