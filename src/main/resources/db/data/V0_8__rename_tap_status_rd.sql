delete
from reference_data
where domain = 'TAP_STATUS';
delete
from reference_data_domain
where code = 'TAP_STATUS';

insert into reference_data_domain(code, description)
values ('TAP_AUTHORISATION_STATUS', 'TAP authorisation status')
on conflict do nothing;

with types as (
    (select *
     from (values ('PENDING', 'Pending', 10, true),
                  ('APPROVED', 'Approved', 20, true),
                  ('CANCELLED', 'Cancelled', 30, true),
                  ('DENIED', 'Denied', 40, true))
              as t(code, description, sequence_number, active)))

insert
into reference_data(domain, code, description, sequence_number, active)
select 'TAP_AUTHORISATION_STATUS', code, description, sequence_number, active
from types
on conflict do nothing;

