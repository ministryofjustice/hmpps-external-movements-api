insert
into reference_data(domain, code, description, sequence_number, active)
select 'ACCOMPANIED_BY', 'NOT_PROVIDED', 'Not provided', 210, true
on conflict do nothing;