insert into reference_data_domain(code, description)
values ('TAP_OCCURRENCE_STATUS', 'TAP occurrence status') on conflict do nothing;

with types as (
    (select *
     from (values ('PENDING', 'Pending', 10, true),
                  ('SCHEDULED', 'Scheduled', 20, true),
                  ('CANCELLED', 'Cancelled', 30, true),
                  ('COMPLETED', 'Completed', 40, true))
              as t(code, description, sequence_number, active)))

insert
into reference_data(domain, code, description, sequence_number, active)
select 'TAP_OCCURRENCE_STATUS', code, description, sequence_number, active
from types on conflict do nothing;