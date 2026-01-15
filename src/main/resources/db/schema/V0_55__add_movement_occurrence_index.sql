create index if not exists idx_temporary_absence_movement_occurrence_id on temporary_absence_movement (occurrence_id) where occurrence_id is not null
;