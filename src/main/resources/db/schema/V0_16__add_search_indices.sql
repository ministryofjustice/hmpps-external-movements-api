create index if not exists idx_taa_prison_code_person_identifier_from_to on temporary_absence_authorisation (prison_code, person_identifier, from_date, to_date);
create index if not exists idx_tao_authorisation_id on temporary_absence_occurrence (authorisation_id);

alter table temporary_absence_authorisation
    add constraint ch_taa_approved_at_approved_by check ( (approved_at is null and approved_by is null) or
                                                      (approved_at is not null and approved_by is not null) );

alter table temporary_absence_occurrence
    add constraint ch_tao_cancelled_at_cancelled_by check ( (cancelled_at is null and cancelled_by is null) or
                                                        (cancelled_at is not null and cancelled_by is not null) );