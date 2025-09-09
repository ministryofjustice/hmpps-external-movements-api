insert into reference_data_domain(code, description)
values ('ACCOMPANIED_BY', 'Accompanied by'),
       ('TRANSPORT', 'Transport'),
       ('TAP_STATUS', 'TAP status')
on conflict do nothing;

with types as (
    (select *
     from (values ('GEOAME', 'GeoAmey', 110, true),
                  ('GROUP4', 'Group 4 Securicor', 120, true),
                  ('HMPS', 'HMP Establishment', 130, true),
                  ('L', 'Prison officer escort (local)', 140, true),
                  ('N', 'Prison officer escort (national)', 150, true),
                  ('P', 'Police escort', 160, true),
                  ('PECS', 'Prisoner escort contractors (other)', 170, true),
                  ('PREM', 'Premier', 180, true),
                  ('REL', 'Reliance', 190, true),
                  ('Z', 'Other', 200, true),
                  ('A', 'Accompanied', 970, false),
                  ('GEOAMEY', 'GeoAmey', 980, false),
                  ('U', 'Unescorted', 990, false))
              as t(code, description, sequence_number, active)))

insert
into reference_data(domain, code, description, sequence_number, active)
select 'ACCOMPANIED_BY', code, description, sequence_number, active
from types
on conflict do nothing;

with types as (
    (select *
     from (values ('AMB', 'Ambulance', 110, true),
                  ('CAR', 'Car', 120, true),
                  ('CAV', 'Category A transport', 130, true),
                  ('COA', 'Coach', 140, true),
                  ('CYCLE', 'Bicycle', 150, true),
                  ('FOOT', 'On foot', 160, true),
                  ('IMM', 'Immigration vehicle', 170, true),
                  ('OD', 'Prisoner driver', 180, true),
                  ('POL', 'Police vehicle', 190, true),
                  ('PUT', 'Public transport', 200, true),
                  ('TAX', 'Taxi', 210, true),
                  ('VAN', 'Van', 220, true),
                  ('TNR', 'Not Required', 990, false))
              as t(code, description, sequence_number, active)))

insert
into reference_data(domain, code, description, sequence_number, active)
select 'TRANSPORT', code, description, sequence_number, active
from types
on conflict do nothing;

with types as (
    (select *
     from (values ('PEN', 'Pending', 10, true),
                  ('APP-SCH', 'Approved (Scheduled)', 20, true),
                  ('APP-UNSCH', 'Approved (Unscheduled)', 30, true),
                  ('CANC', 'Cancelled', 40, true),
                  ('DEN', 'Denied', 50, true))
              as t(code, description, sequence_number, active)))

insert
into reference_data(domain, code, description, sequence_number, active)
select 'TAP_STATUS', code, description, sequence_number, active
from types
on conflict do nothing;
