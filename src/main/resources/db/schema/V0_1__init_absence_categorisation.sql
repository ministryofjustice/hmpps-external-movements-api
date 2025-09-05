create table if not exists reference_data
(
    id              bigserial    not null,
    constraint pk_reference_data primary key (id),
    domain          varchar(32)  not null,
    code            varchar(16)  not null,
    description     varchar(255) not null,
    hint_text       varchar(255),
    constraint uq_domain_code unique (domain, code),
    sequence_number int          not null,
    constraint uq_domain_sequence_number unique (domain, sequence_number),
    active          boolean      not null
);