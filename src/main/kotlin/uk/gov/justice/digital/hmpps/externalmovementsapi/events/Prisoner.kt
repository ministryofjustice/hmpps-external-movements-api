package uk.gov.justice.digital.hmpps.externalmovementsapi.events

import uk.gov.justice.digital.hmpps.externalmovementsapi.context.DataSource

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
