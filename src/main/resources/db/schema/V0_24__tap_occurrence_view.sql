create or replace function get_occurrence_status_code(_occurrence_id uuid) returns varchar as
$$
declare
    _authorisation_id          uuid;
    _return_by                 timestamp;
    _out_movement_id           uuid;
    _in_movement_id            uuid;
    _authorisation_status_code varchar;
    _is_cancelled              boolean;
begin
    select tao.authorisation_id, tao.return_by, case when cancelled_at is not null then true else false end
    into _authorisation_id, _return_by, _is_cancelled
    from temporary_absence_occurrence tao
    where tao.id = _occurrence_id;

    select tam.id
    into _out_movement_id
    from temporary_absence_movement tam
    where tam.occurrence_id = _occurrence_id
      and tam.direction = 'OUT';

    select tam.id
    into _in_movement_id
    from temporary_absence_movement tam
    where tam.occurrence_id = _occurrence_id
      and tam.direction = 'IN';

    if (_out_movement_id is not null and _in_movement_id is null) then
        if (_return_by < current_timestamp)
        then
            return 'OVERDUE';
        else
            return 'IN_PROGRESS';
        end if;
    end if;

    if (_in_movement_id is not null) then
        return 'COMPLETED';
    end if;

    select code
    into _authorisation_status_code
    from temporary_absence_authorisation taa
             join reference_data taas on taas.id = taa.status_id
    where taa.id = _authorisation_id
      and taas.domain = 'TAP_AUTHORISATION_STATUS';

    if (_authorisation_status_code <> 'APPROVED')
    then
        return _authorisation_status_code;
    end if;

    if (_out_movement_id is null and _in_movement_id is null and _authorisation_status_code = 'APPROVED')
    then
        if (_is_cancelled = true)
        then
            return 'CANCELLED';
        end if;
        if (_return_by >= current_timestamp)
        then
            return 'SCHEDULED';
        else
            return 'EXPIRED';
        end if;
    end if;

    return 'UNKNOWN';

end;
$$ language plpgsql;

create or replace view temporary_absence_occurrence_status as
(
select tao.id                                            as occurrence_id,
       (select id
        from reference_data
        where domain = 'TAP_OCCURRENCE_STATUS'
          and code = get_occurrence_status_code(tao.id)) as status_id
from temporary_absence_occurrence tao
    );