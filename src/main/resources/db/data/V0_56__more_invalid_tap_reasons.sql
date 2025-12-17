with types as (
    (select *
     from (values ('ETRLR', '(Invalid absence reason) Recall from Emergency Temporary Release', 9970, false),
                  ('ETRB', '(Invalid absence reason) Breach of Emergency Temporary Release', 9960, false),
                  ('MED', '(Invalid absence reason) Medical', 9950,false)
           ) as t(code, description, sequence_number, active)))
insert
into reference_data(domain, code, description, sequence_number, active)
select 'ABSENCE_REASON', code, description, sequence_number, active
from types on conflict do nothing;