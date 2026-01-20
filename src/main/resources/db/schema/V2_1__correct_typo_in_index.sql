-- V2_0 script typo added this to tap.authorisation table
drop index if exists tap.idx_tap_occurrence_prison_person_start_end;
create index if not exists idx_tap_occurrence_prison_person_start_end on tap.occurrence (prison_code, person_identifier, start, "end");

alter table tap.reference_data
    drop column legacy_id;
alter table tap.reference_data
    set schema public;

create table if not exists tap.prison_absence_categorisation
(
    prison_code     varchar(6)   not null,
    domain_code     varchar(32)  not null,
    code            varchar(16)  not null,
    description     varchar(255) not null,
    active          boolean      not null,
    sequence_number int          not null,
    constraint pk_prison_absence_categorisation primary key (prison_code, domain_code, code),
    constraint fk_prison_absence_categorisation_domain_code foreign key (domain_code) references reference_data_domain (code)
);

create or replace function tap.merge_prison_absence_categorisation() returns trigger as
$$
begin
    insert into tap.prison_absence_categorisation(prison_code, domain_code, code, description, sequence_number, active)
    select new.prison_code, 'ABSENCE_TYPE', code, description, sequence_number, active
    from tap.absence_type
    where id = new.absence_type_id
    union
    select new.prison_code, 'ABSENCE_SUB_TYPE', code, description, sequence_number, active
    from tap.absence_sub_type
    where id = new.absence_sub_type_id
    union
    select new.prison_code, 'ABSENCE_REASON_CATEGORY', code, description, sequence_number, active
    from tap.absence_reason_category
    where id = new.absence_reason_category_id
    union
    select new.prison_code, 'ABSENCE_REASON', code, description, sequence_number, active
    from tap.absence_reason
    where id = new.absence_reason_id
    on conflict do nothing;
    return new;
end;
$$ language plpgsql;

create or replace trigger insert_authorisation_prison_absence_categorisation
    after insert
    on tap.authorisation
    for each row
execute function tap.merge_prison_absence_categorisation();

create or replace trigger update_authorisation_prison_absence_categorisation
    after update
    on tap.authorisation
    for each row
    when (old.absence_type_id is distinct from new.absence_type_id or
          old.absence_sub_type_id is distinct from new.absence_sub_type_id or
          old.absence_reason_category_id is distinct from new.absence_reason_category_id or
          old.absence_reason_id is distinct from new.absence_reason_id)
execute function tap.merge_prison_absence_categorisation();

create or replace trigger insert_occurrence_prison_absence_categorisation
    after insert
    on tap.occurrence
    for each row
execute function tap.merge_prison_absence_categorisation();

create or replace trigger update_authorisation_prison_absence_categorisation
    after update
    on tap.occurrence
    for each row
    when (old.absence_type_id is distinct from new.absence_type_id or
          old.absence_sub_type_id is distinct from new.absence_sub_type_id or
          old.absence_reason_category_id is distinct from new.absence_reason_category_id or
          old.absence_reason_id is distinct from new.absence_reason_id)
execute function tap.merge_prison_absence_categorisation();