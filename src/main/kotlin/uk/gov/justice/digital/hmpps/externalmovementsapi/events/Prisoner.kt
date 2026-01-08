package uk.gov.justice.digital.hmpps.externalmovementsapi.events

import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.PrisonerUpdated.Companion.EVENT_TYPE

data class PrisonerUpdatedInformation(
  val nomsNumber: String,
  val categoriesChanged: Set<String>,
) : AdditionalInformation {
  override val source: DataSource = DataSource.NOMIS

  companion object {
    val CATEGORIES_OF_INTEREST = setOf("PERSONAL_DETAILS", "LOCATION")
  }
}

data class PrisonerUpdated(
  override val additionalInformation: PrisonerUpdatedInformation,
  override val personReference: PersonReference,
) : DomainEvent<PrisonerUpdatedInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = "Detail about the prisoner have changed"

  companion object {
    const val EVENT_TYPE: String = "prisoner-offender-search.prisoner.updated"
  }
}

data class PrisonerMergedInformation(val removedNomsNumber: String, val nomsNumber: String) : AdditionalInformation {
  override val source: DataSource = DataSource.NOMIS
}

data class PrisonerMerged(
  override val additionalInformation: PrisonerMergedInformation,
  override val personReference: PersonReference,
) : DomainEvent<PrisonerMergedInformation> {
  override val eventType: String = EVENT_TYPE
  override val description: String = DESCRIPTION

  companion object {
    const val EVENT_TYPE: String = "prison-offender-events.prisoner.merged"
    const val DESCRIPTION: String = "Prisoner records merged"
  }
}
