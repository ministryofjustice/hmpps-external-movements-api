package uk.gov.justice.digital.hmpps.externalmovementsapi.model.integration

import java.util.UUID

object IntegrationUrlBuilder {
  lateinit var baseUrl: String

  fun authorisationUrl(id: UUID): String = "$baseUrl/integrations/temporary-absence-authorisations/$id"

  fun authorisationOccurrencesUrl(id: UUID): String = "$baseUrl/integrations/temporary-absence-authorisations/$id/occurrences"

  fun occurrenceUrl(id: UUID): String = "$baseUrl/integrations/temporary-absence-occurrences/$id"

  fun occurrenceMovementsUrl(id: UUID): String = "$baseUrl/integrations/temporary-absence-occurrences/$id/movements"

  fun movementUrl(id: UUID): String = "$baseUrl/integrations/temporary-absence-movements/$id"
}
