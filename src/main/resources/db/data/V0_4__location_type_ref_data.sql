insert into reference_data_domain(code, description)
values ('LOCATION_TYPE', 'Location type')
on conflict do nothing;

with types as (
    (select *
     from (values ('CORP', 'Business or community', 10, true,
                   'These are places of business or community settings such as workplaces, approved premises, hospitals and colleges. This does not have to be a specific addresses but can be a whole area (for example, York).'),
                  ('OFF', 'Personal', 20, true,
                   'These are locations personal to the prisoner. For example, their home address, relatives’ addresses or partner’s address.'),
                  ('AGY', 'Official', 30, true,
                   'These are official locations associated with the justice system. For example, probation offices, police stations and courts.'),
                  ('OTHER', 'Other - NOMIS only type', 90, false, null))
              as t(code, description, sequence_number, active, hint_text)))
insert
into reference_data(domain, code, description, sequence_number, active, hint_text)
select 'LOCATION_TYPE', code, description, sequence_number, active, hint_text
from types
on conflict do nothing;