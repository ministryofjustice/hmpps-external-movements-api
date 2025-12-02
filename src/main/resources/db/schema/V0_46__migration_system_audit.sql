create table if not exists migration_system_audit
(
    id         uuid        not null,
    created_at timestamp   not null,
    created_by varchar(64) not null,
    updated_at timestamp,
    updated_by varchar(64),
    constraint pk_migration_system_audit primary key (id)
);