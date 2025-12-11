with types as (
    (select *
     from (values ('ELR', '(Invalid absence reason) Recall from End of Custody Licence (ECL)', 9980, false),
                  ('IR', '(Invalid absence reason) Intermittent Custody Release', 9990,false)
           ) as t(code, description, sequence_number, active)))

insert
into reference_data(domain, code, description, sequence_number, active)
select 'ABSENCE_REASON', code, description, sequence_number, active
from types on conflict do nothing;