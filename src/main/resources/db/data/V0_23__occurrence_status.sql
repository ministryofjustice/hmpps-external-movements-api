update reference_data
set description = 'To be reviewed'
where domain = 'TAP_AUTHORISATION_STATUS'
  and code = 'PENDING';

update reference_data
set code        = 'WITHDRAWN',
    description = 'Withdrawn'
where domain = 'TAP_AUTHORISATION_STATUS'
  and code = 'CANCELLED';

insert into reference_data_domain(code, description)
values ('TAP_OCCURRENCE_STATUS', 'TAP occurrence status')
on conflict do nothing;

with types as (
    (select *
     from (values ('PENDING', 'To be reviewed', 10, true),
                  ('SCHEDULED', 'Scheduled', 20, true),
                  ('IN_PROGRESS', 'In progress', 30, true),
                  ('COMPLETED', 'Completed', 40, true),
                  ('OVERDUE', 'Overdue', 50, true),
                  ('EXPIRED', 'Expired', 60, true),
                  ('CANCELLED', 'Cancelled', 70, true),
                  ('DENIED', 'Denied', 80, true),
                  ('WITHDRAWN', 'Withdrawn', 90, true))
              as t(code, description, sequence_number, active)))

insert
into reference_data(domain, code, description, sequence_number, active)
select 'TAP_OCCURRENCE_STATUS', code, description, sequence_number, active
from types
on conflict do nothing;