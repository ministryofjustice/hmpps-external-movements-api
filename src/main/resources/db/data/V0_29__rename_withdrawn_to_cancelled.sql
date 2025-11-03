update reference_data
set code        = 'CANCELLED',
    description = 'Cancelled'
where domain = 'TAP_AUTHORISATION_STATUS'
  and code = 'WITHDRAWN';

delete
from reference_data
where domain = 'TAP_OCCURRENCE_STATUS'
  and code = 'WITHDRAWN';