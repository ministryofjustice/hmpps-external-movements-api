update reference_data
set hint_text = 'A short release in response to exceptional or personal circumstances, such as medical appointments, or wider criminal justice needs.'
where domain = 'ABSENCE_SUB_TYPE' and code = 'SPL';
