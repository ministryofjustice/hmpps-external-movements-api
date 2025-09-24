delete
from reference_data
where domain = 'TAP_AUTHORISATION_STATUS';

with types as (
    (select *
     from (values ('PENDING', 'Pending', 10, true),
                  ('APPROVED', 'Approved', 20, true),
                  ('DENIED', 'Denied', 30, true),
                  ('WITHDRAWN', 'Withdrawn', 40, true))
              as t(code, description, sequence_number, active)))

insert
into reference_data(domain, code, description, sequence_number, active)
select 'TAP_AUTHORISATION_STATUS', code, description, sequence_number, active
from types
on conflict do nothing;