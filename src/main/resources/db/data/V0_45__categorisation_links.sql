insert into reference_data_link(reference_data_id_1, reference_data_id_2, sequence_number)
select rd1.id, rd2.id, 1
from reference_data rd1
         join reference_data rd2 on rd1.code = rd2.code
where rd1.domain = 'ABSENCE_REASON_CATEGORY'
  and rd1.code in (
                   'FB',
                   'ET',
                   'R3',
                   'PAP',
                   'YOTR'
    )
  and rd2.domain = 'ABSENCE_REASON'
  and rd2.code in (
                   'FB',
                   'ET',
                   'R3',
                   'PAP',
                   'YOTR'
    )
on conflict do nothing;
