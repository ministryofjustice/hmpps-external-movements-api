insert into reference_data_domain(code, description)
values ('ABSENCE_TYPE', 'Absence type'),
       ('ABSENCE_SUB_TYPE', 'Absence sub type'),
       ('ABSENCE_REASON_CATEGORY', 'Absence reason category'),
       ('ABSENCE_REASON', 'Absence reason')
on conflict do nothing;

with types as (
    (select *
     from (values ('SR', 'Standard ROTL (Release on Temporary Licence)', 10, true),
                  ('RR', 'Restricted ROTL (Release on Temporary Licence)', 20, true),
                  ('PP', 'Police production', 30, true),
                  ('SE', 'Security escort', 40, true),
                  ('YT', 'Youth temporary release', 50, true),
                  ('ETL23', 'Extended Temporary Licence 2023', 80, false),
                  ('ETR23', 'Extended Temporary Release 2023', 90,
                   false)) as t(code, description, sequence_number, active)))

insert
into reference_data(domain, code, description, sequence_number, active)
select 'ABSENCE_TYPE', code, description, sequence_number, active
from types
on conflict do nothing;

with links as (select id,
                      case
                          when code in ('SR', 'RR', 'YT') then 'ABSENCE_SUB_TYPE'
                          when code = 'SE' then 'ABSENCE_REASON' end as domain
               from reference_data
               where domain = 'ABSENCE_TYPE')
insert
into reference_data_domain_link(id, domain)
select id, domain
from links
where domain is not null
on conflict do nothing;

with sub_types as (
    (select *
     from (values ('CRL', 'CRL (Childcare Resettlement Licence)', 110, true,
                   'To help prisoners prepare for parenting when they are released and support ties between primary carers and their children.'),
                  ('RDR', 'RDR (Resettlement Day Release)', 120, true,
                   'For prisoners to carry out activities linked to objectives in their sentence plan.'),
                  ('ROR', 'ROR (Resettlement Overnight Release)', 130, true,
                   'For prisoners to spend time at their release address to re-establish links with family and the local community.'),
                  ('SPL', 'SPL (Special Purpose Licence)', 140, true,
                   'For prisoners to spend time at their release address to re-establish links with family and the local community.'),
                  ('PP', 'Police production', 150, true, null),
                  ('SE', 'Security escort', 160, true, null),
                  ('YTRA', 'Accommodation', 170, true, null),
                  ('YTRC', 'Case work and transitions', 180, true, null),
                  ('YTRE', 'Education, training and employment', 190, true, null),
                  ('YTRF', 'Family', 200, true, null),
                  ('ETL23', 'Extended Temporary Licence 2023', 980, false, null),
                  ('ETR23', 'Extended Temporary Absence 2023', 980, false,
                   null)) as t(code, description, sequence_number, active, hint_text)))

insert
into reference_data(domain, code, description, sequence_number, active, hint_text)
select 'ABSENCE_SUB_TYPE', code, description, sequence_number, active, hint_text
from sub_types
on conflict do nothing;

with links as (select id,
                      case
                          when code in ('RDR') then 'ABSENCE_REASON_CATEGORY'
                          when code in ('SPL', 'YTRA', 'YTRC', 'YTRE', 'YTRF') then 'ABSENCE_REASON' end as domain
               from reference_data
               where domain = 'ABSENCE_SUB_TYPE')
insert
into reference_data_domain_link(id, domain)
select id, domain
from links
where domain is not null
on conflict do nothing;


with categories as (
    (select *
     from (values ('FB', 'Accommodation-related', 1110, true),
                  ('ET', 'Education or training', 1120, true),
                  ('R3', 'Maintaining family ties', 1130, true),
                  ('PW', 'Paid work', 1140, true),
                  ('PAP', 'Prisoner apprenticeships pathway', 1150, true),
                  ('UW', 'Unpaid work', 1160, true),
                  ('YOTR', 'Other temporary release linked to sentence or resettlement plan', 1170,
                   true)) as t(code, description, sequence_number, active)))

insert
into reference_data(domain, code, description, sequence_number, active)
select 'ABSENCE_REASON_CATEGORY', code, description, sequence_number, active
from categories
on conflict do nothing;

with links as (select id,
                      case
                          when code in ('PW', 'UW') then 'ABSENCE_REASON' end as domain
               from reference_data
               where domain = 'ABSENCE_REASON_CATEGORY')
insert
into reference_data_domain_link(id, domain)
select id, domain
from links
where domain is not null
on conflict do nothing;

with reasons as (
    (select *
     from (values ('FB', 'Accommodation-related', 1110, true),
                  ('ET', 'Education or training', 1120, true),
                  ('R3', 'Maintaining family ties', 1130, true),
                  ('PAP', 'Prisoner apprenticeships pathway', 1140, true),
                  ('YOTR', 'Other temporary release linked to sentence or resettlement plan', 1150, true),
                  ('R16', 'Paid work - Agriculture and horticulture', 1160, true),
                  ('R12', 'Paid work - Catering and hospitality', 1170, true),
                  ('R14', 'Paid work - Construction and recycling', 1180, true),
                  ('R15', 'Paid work - IT and communication', 1190, true),
                  ('R11', 'Paid work - Manufacturing', 1200, true),
                  ('R10', 'Paid work - Retail and wholesale', 1210, true),
                  ('R13', 'Paid work - Transportation and storage', 1220, true),
                  ('R17', 'Paid work - Other', 1230, true),
                  ('R24', 'Unpaid work - Agriculture and horticulture', 1240, true),
                  ('R20', 'Unpaid work - Catering and hospitality', 1250, true),
                  ('R22', 'Unpaid work - Construction and recycling', 1260, true),
                  ('R23', 'Unpaid work - IT and communication', 1270, true),
                  ('R19', 'Unpaid work - Manufacturing', 1280, true),
                  ('R18', 'Unpaid work - Retail and wholesale', 1290, true),
                  ('R21', 'Unpaid work - Transportation and storage', 1300, true),
                  ('R25', 'Unpaid work - Other', 1310, true),
                  ('LTX', 'Court, legal, police or prison transfer', 1320, true),
                  ('C3', 'Death or funeral', 1330, true),
                  ('C6', 'Inpatient medical or dental appointment', 1340, true),
                  ('C5', 'Outpatient medical or dental appointment', 1350, true),
                  ('C7', 'Visit a dying relative', 1360, true),
                  ('C4', 'Wedding or civil ceremony', 1370, true),
                  ('4', 'Other compassionate reason', 1380, true),
                  ('SE', 'Other security escort', 1390, true),
                  ('YRDR', 'RDR (Resettlement Day Release)', 1400, true),
                  ('RO', 'ROR (Resettlement Overnight Release)', 1410, true),
                  ('YMI', 'Meeting and interviews', 1420, true),
                  ('20', 'Other temporary release linked to sentence or resettlement plan', 1430, true),
                  ('OPA', 'Outside prison activity', 1440, true),
                  ('PC', 'Police Production', 1450, true),
                  ('3', 'CRL (Childcare Resettlement Licence)', 1460, true),
                  ('C9', 'Attend Religious Service', 1470, true),
                  ('TRNTAP', 'Transfer Via Temporary Release', 1480, true),
                  ('F4', 'Unescorted Court Appearance', 1490, true),
                  ('10', 'Adventure and social welfare', 9400, false),
                  ('FC', 'Adventure training', 9410, false),
                  ('8', 'Community service volunteer', 9420, false),
                  ('R7', 'Community service volunteer', 9430, false),
                  ('FR', 'Deport of foreign national under early removal scheme', 9440, false),
                  ('FE', 'Driving lessons', 9450, false),
                  ('F2', 'Education', 9460, false),
                  ('ETR3', 'Emergency temporary release - end of sentence', 9470, false),
                  ('ETR1', 'Emergency temporary release - Pregnant/MBU', 9480, false),
                  ('ETR2', 'Emergency temporary release - vulnerable', 9490, false),
                  ('13', 'Employment interview', 9500, false),
                  ('EL', 'ECL (End of Custody Licence)', 9510, false),
                  ('ETL23', 'Extended temporary release 2023', 9520, false),
                  ('ETR23', 'Extended temporary release 2023', 9530, false),
                  ('7', 'External education classes', 9540, false),
                  ('6', 'External sporting activity', 9550, false),
                  ('FD', 'External team sports', 9560, false),
                  ('2', 'Funeral', 9570, false),
                  ('SP', 'Helping police with enquiries', 9580, false),
                  ('IM', 'Immigration Hearing', 9590, false),
                  ('ST', 'Interprison transfer', 9600, false),
                  ('11', 'Interview for half-way house', 9610, false),
                  ('F9', 'Interview for hostel accommodation', 9620, false),
                  ('R4', 'Interview for hostel accommodation', 9630, false),
                  ('R1', 'Job interview', 9640, false),
                  ('F8', 'Job interview (under 12m)', 9650, false),
                  ('I1', 'Local visit (incentive scheme)', 9660, false),
                  ('14', 'Long home leave', 9670, false),
                  ('18', 'Marriage', 9680, false),
                  ('FF', 'Opening bank accounts', 9690, false),
                  ('21', 'Other (i.e. IPV, ID parades etc)', 9700, false),
                  ('C8', 'Other special purpose', 9710, false),
                  ('5', 'Outside hospital attendance', 9720, false),
                  ('R9', 'Overnight resettlement unit/pres hostel', 9730, false),
                  ('R2', 'Paid work placements', 9740, false),
                  ('PWP', 'Paid work placements (youth)', 9750, false),
                  ('17', 'Pre-parole leave', 9760, false),
                  ('R6', 'Pre-parole release', 9770, false),
                  ('C1', 'Primary carer', 9780, false),
                  ('R8', 'Probation service workshop', 9790, false),
                  ('9', 'Probation service workshops', 9800, false),
                  ('TAP', 'ROTL', 9810, false),
                  ('F3', 'Reparation projects', 9820, false),
                  ('R5', 'Seeking other accommodation', 9830, false),
                  ('FA', 'Seeking other accommodation (under 12m)', 9840, false),
                  ('16', 'Short home leave', 9850, false),
                  ('15', 'Terminal home leave', 9860, false),
                  ('F1', 'Training', 9870, false),
                  ('UPW', 'Unpaid work placements', 9880, false),
                  ('C2', 'Urgent domestic or family needs', 9890, false),
                  ('1', 'Visit dying relative', 9900, false),
                  ('F6', 'Visit legal adviser (unescorted)', 9910, false),
                  ('12', 'Visit to outside hostel for assessment', 9920, false),
                  ('F7', 'Working out scheme', 9930, false)) as t(code, description, sequence_number, active)))

insert
into reference_data(domain, code, description, sequence_number, active)
select 'ABSENCE_REASON', code, description, sequence_number, active
from reasons
on conflict do nothing;


with links as (select rd1.id                                                         rd1_id,
                      rd2.id                                                         rd2_id,
                      row_number() over (partition by rd1.code order by rd2.code) as row_num
               from reference_data rd1
                        cross join reference_data rd2
               where rd1.domain = 'ABSENCE_TYPE'
                 and rd1.code in ('SR', 'RR')
                 and rd2.domain = 'ABSENCE_SUB_TYPE'
                 and rd2.code in ('CRL', 'RDR', 'ROR', 'SPL'))
insert
into reference_data_link(reference_data_id_1, reference_data_id_2, sequence_number)
select rd1_id, rd2_id, row_num * 10
from links
on conflict do nothing;

with links as (select rd1.id                                                         rd1_id,
                      rd2.id                                                         rd2_id,
                      row_number() over (partition by rd1.code order by rd2.code) as row_num
               from reference_data rd1
                        cross join reference_data rd2
               where rd1.domain = 'ABSENCE_TYPE'
                 and rd1.code in ('YT')
                 and rd2.domain = 'ABSENCE_SUB_TYPE'
                 and rd2.code in ('YTRA', 'YTRC', 'YTRE', 'YTRF'))
insert
into reference_data_link(reference_data_id_1, reference_data_id_2, sequence_number)
select rd1_id, rd2_id, row_num * 10
from links
on conflict do nothing;

with links as (select rd1.id                                                                    rd1_id,
                      rd2.id                                                                    rd2_id,
                      row_number() over (partition by rd1.code order by rd2.sequence_number) as row_num
               from reference_data rd1
                        cross join reference_data rd2
               where rd1.domain = 'ABSENCE_TYPE'
                 and rd1.code in ('SE')
                 and rd2.domain = 'ABSENCE_REASON'
                 and rd2.code in ('LTX', 'C3', 'C6', 'C5', 'C7', 'C4', '4', 'SE'))
insert
into reference_data_link(reference_data_id_1, reference_data_id_2, sequence_number)
select rd1_id, rd2_id, row_num * 10
from links
on conflict do nothing;

with links as (select rd1.id                                                                    rd1_id,
                      rd2.id                                                                    rd2_id,
                      row_number() over (partition by rd1.code order by rd2.sequence_number) as row_num
               from reference_data rd1
                        cross join reference_data rd2
               where rd1.domain = 'ABSENCE_SUB_TYPE'
                 and rd1.code in ('RDR')
                 and rd2.domain = 'ABSENCE_REASON_CATEGORY'
                 and rd2.code in ('FB', 'ET', 'R3', 'PW', 'PAP', 'UW', 'YOTR'))
insert
into reference_data_link(reference_data_id_1, reference_data_id_2, sequence_number)
select rd1_id, rd2_id, row_num * 10
from links
on conflict do nothing;

with links as (select rd1.id                                                                    rd1_id,
                      rd2.id                                                                    rd2_id,
                      row_number() over (partition by rd1.code order by rd2.sequence_number) as row_num
               from reference_data rd1
                        cross join reference_data rd2
               where rd1.domain = 'ABSENCE_REASON_CATEGORY'
                 and rd1.code in ('PW')
                 and rd2.domain = 'ABSENCE_REASON'
                 and rd2.code in ('R16', 'R12', 'R14', 'R15', 'R11', 'R10', 'R13', 'R17'))
insert
into reference_data_link(reference_data_id_1, reference_data_id_2, sequence_number)
select rd1_id, rd2_id, row_num * 10
from links
on conflict do nothing;

with links as (select rd1.id                                                                    rd1_id,
                      rd2.id                                                                    rd2_id,
                      row_number() over (partition by rd1.code order by rd2.sequence_number) as row_num
               from reference_data rd1
                        cross join reference_data rd2
               where rd1.domain = 'ABSENCE_REASON_CATEGORY'
                 and rd1.code in ('UW')
                 and rd2.domain = 'ABSENCE_REASON'
                 and rd2.code in ('R24', 'R20', 'R22', 'R23', 'R19', 'R18', 'R21', 'R25'))
insert
into reference_data_link(reference_data_id_1, reference_data_id_2, sequence_number)
select rd1_id, rd2_id, row_num * 10
from links
on conflict do nothing;

with links as (select rd1.id                                                                    rd1_id,
                      rd2.id                                                                    rd2_id,
                      row_number() over (partition by rd1.code order by rd2.sequence_number) as row_num
               from reference_data rd1
                        cross join reference_data rd2
               where rd1.domain = 'ABSENCE_SUB_TYPE'
                 and rd1.code in ('SPL')
                 and rd2.domain = 'ABSENCE_REASON'
                 and rd2.code in ('LTX', 'C3', 'C6', 'C5', 'C7', 'C4', '4'))
insert
into reference_data_link(reference_data_id_1, reference_data_id_2, sequence_number)
select rd1_id, rd2_id, row_num * 10
from links
on conflict do nothing;

with links as (select rd1.id                                                                    rd1_id,
                      rd2.id                                                                    rd2_id,
                      row_number() over (partition by rd1.code order by rd2.sequence_number) as row_num
               from reference_data rd1
                        cross join reference_data rd2
               where rd1.domain = 'ABSENCE_SUB_TYPE'
                 and rd1.code in ('YTRA')
                 and rd2.domain = 'ABSENCE_REASON'
                 and rd2.code in ('YRDR', 'RO'))
insert
into reference_data_link(reference_data_id_1, reference_data_id_2, sequence_number)
select rd1_id, rd2_id, row_num * 10
from links
on conflict do nothing;

with links as (select rd1.id                                                                    rd1_id,
                      rd2.id                                                                    rd2_id,
                      row_number() over (partition by rd1.code order by rd2.sequence_number) as row_num
               from reference_data rd1
                        cross join reference_data rd2
               where rd1.domain = 'ABSENCE_SUB_TYPE'
                 and rd1.code in ('YTRC')
                 and rd2.domain = 'ABSENCE_REASON'
                 and rd2.code in ('YMI', '20', 'OPA'))
insert
into reference_data_link(reference_data_id_1, reference_data_id_2, sequence_number)
select rd1_id, rd2_id, row_num * 10
from links
on conflict do nothing;

with links as (select rd1.id                                                                    rd1_id,
                      rd2.id                                                                    rd2_id,
                      row_number() over (partition by rd1.code order by rd2.sequence_number) as row_num
               from reference_data rd1
                        cross join reference_data rd2
               where rd1.domain = 'ABSENCE_SUB_TYPE'
                 and rd1.code in ('YTRE')
                 and rd2.domain = 'ABSENCE_REASON'
                 and rd2.code in ('ET', 'PAP'))
insert
into reference_data_link(reference_data_id_1, reference_data_id_2, sequence_number)
select rd1_id, rd2_id, row_num * 10
from links
on conflict do nothing;

with links as (select rd1.id                                 rd1_id,
                      rd2.id                                 rd2_id,
                      case
                          when rd2.code = 'C3' then 10
                          when rd2.code = 'R3' then 20
                          when rd2.code = 'C7' then 30
                          when rd2.code = 'C4' then 40
                          when rd2.code = '4' then 50 end as row_num
               from reference_data rd1
                        cross join reference_data rd2
               where rd1.domain = 'ABSENCE_SUB_TYPE'
                 and rd1.code in ('YTRF')
                 and rd2.domain = 'ABSENCE_REASON'
                 and rd2.code in ('C3', 'R3', 'C7', 'C4', '4'))
insert
into reference_data_link(reference_data_id_1, reference_data_id_2, sequence_number)
select rd1_id, rd2_id, row_num
from links
on conflict do nothing;

with links as (select rd1.id rd1_id,
                      rd2.id rd2_id,
                      10 as  row_num
               from reference_data rd1
                        cross join reference_data rd2
               where rd1.domain = 'ABSENCE_TYPE'
                 and rd1.code in ('PP')
                 and rd2.domain = 'ABSENCE_SUB_TYPE'
                 and rd2.code in ('PP'))
insert
into reference_data_link(reference_data_id_1, reference_data_id_2, sequence_number)
select rd1_id, rd2_id, row_num
from links
on conflict do nothing;

with links as (select rd1.id rd1_id,
                      rd2.id rd2_id,
                      10 as  row_num
               from reference_data rd1
                        cross join reference_data rd2
               where rd1.domain = 'ABSENCE_SUB_TYPE'
                 and rd1.code in ('PP')
                 and rd2.domain = 'ABSENCE_REASON'
                 and rd2.code in ('PC'))
insert
into reference_data_link(reference_data_id_1, reference_data_id_2, sequence_number)
select rd1_id, rd2_id, row_num
from links
on conflict do nothing;

with links as (select rd1.id rd1_id,
                      rd2.id rd2_id,
                      10 as  row_num
               from reference_data rd1
                        cross join reference_data rd2
               where rd1.domain = 'ABSENCE_SUB_TYPE'
                 and rd1.code in ('CRL')
                 and rd2.domain = 'ABSENCE_REASON'
                 and rd2.code in ('3'))
insert
into reference_data_link(reference_data_id_1, reference_data_id_2, sequence_number)
select rd1_id, rd2_id, row_num
from links
on conflict do nothing;

with links as (select rd1.id rd1_id,
                      rd2.id rd2_id,
                      10 as  row_num
               from reference_data rd1
                        cross join reference_data rd2
               where rd1.domain = 'ABSENCE_SUB_TYPE'
                 and rd1.code in ('ROR')
                 and rd2.domain = 'ABSENCE_REASON'
                 and rd2.code in ('RO'))
insert
into reference_data_link(reference_data_id_1, reference_data_id_2, sequence_number)
select rd1_id, rd2_id, row_num
from links
on conflict do nothing;


with links as (select rd1.id rd1_id,
                      rd2.id rd2_id,
                      10 as  row_num
               from reference_data rd1
                        cross join reference_data rd2
               where rd1.domain = 'ABSENCE_SUB_TYPE'
                 and rd1.code in ('ROR')
                 and rd2.domain = 'ABSENCE_REASON'
                 and rd2.code in ('RO'))
insert
into reference_data_link(reference_data_id_1, reference_data_id_2, sequence_number)
select rd1_id, rd2_id, row_num
from links
on conflict do nothing;