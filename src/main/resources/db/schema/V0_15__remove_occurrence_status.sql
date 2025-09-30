alter table temporary_absence_occurrence
    drop column if exists status_id;

alter table temporary_absence_occurrence_audit
    drop column if exists status_id;