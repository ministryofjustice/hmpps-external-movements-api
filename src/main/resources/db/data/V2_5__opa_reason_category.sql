insert into tap.absence_reason_category(code, description, sequence_number, active, next_domain)
values ('OPA', 'Outside prison activity', 1180, true, null)
on conflict do nothing
;

insert into tap.absence_categorisation_link(domain_1, id_1, domain_2, id_2, sequence_number)
select 'ABSENCE_SUB_TYPE', st.id, 'ABSENCE_REASON_CATEGORY', rc.id, 80
from tap.absence_sub_type st,
     tap.absence_reason_category rc
where st.code = 'RDR'
  and rc.code = 'OPA'
on conflict do nothing
;

insert into tap.absence_categorisation_link(domain_1, id_1, domain_2, id_2, sequence_number)
select 'ABSENCE_REASON_CATEGORY', rc.id, 'ABSENCE_REASON', r.id, 1
from tap.absence_reason_category rc, tap.absence_reason r
where rc.code = 'OPA' and r.code = 'OPA'
on conflict do nothing
;
