alter table tap.absence_sub_type
    add column groups text[] not null default array[]::varchar[]
;