package uk.gov.justice.digital.hmpps.externalmovementsapi.events

import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import java.util.UUID

data class ExternalMovementInformation(
  override val id: UUID,
  override val source: DataSource,
) : AdditionalInformation, IdInformation
