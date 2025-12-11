insert
into reference_data(domain, code, description, sequence_number, active)
values ('TAP_AUTHORISATION_STATUS', 'EXPIRED', 'Expired', 50, true)
on conflict do nothing;