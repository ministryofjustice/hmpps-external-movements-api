create index if not exists idx_tap_occurrence_person_start_end on tap.occurrence (person_identifier, start, "end")
;