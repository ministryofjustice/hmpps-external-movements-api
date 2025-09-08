create table if not exists reference_data_domain
(
    code        varchar(32)  not null,
    description varchar(255) not null,
    constraint pk_reference_data_domain primary key (code)
);

create table if not exists reference_data
(
    id              bigserial    not null,
    domain          varchar(32)  not null,
    code            varchar(16)  not null,
    description     varchar(255) not null,
    hint_text       varchar(255),
    sequence_number int          not null,
    active          boolean      not null,
    constraint pk_reference_data primary key (id),
    constraint fk_reference_data_domain foreign key (domain) references reference_data_domain (code),
    constraint uq_domain_code unique (domain, code),
    constraint uq_domain_sequence_number unique (domain, sequence_number)
);

create table if not exists reference_data_domain_link
(
    id     bigint      not null,
    domain varchar(32) not null,
    constraint pk_reference_data_domain_link primary key (id),
    constraint fk_reference_data_domain_link_reference_data foreign key (id) references reference_data (id),
    constraint fk_reference_data_domain_link_domain foreign key (domain) references reference_data_domain (code)
);

create table if not exists reference_data_link
(
    id                  bigserial not null,
    reference_data_id_1 bigint    not null,
    reference_data_id_2 bigint    not null,
    sequence_number     int       not null,
    constraint pk_reference_data_link primary key (id),
    constraint fk_reference_data_one foreign key (reference_data_id_1) references reference_data (id),
    constraint fk_reference_data_two foreign key (reference_data_id_2) references reference_data (id),
    constraint uq_reference_data_link_sequence_number unique (reference_data_id_1, sequence_number)
);