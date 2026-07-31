package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.occurrence.TemporaryAbsenceOccurrenceRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.domain.occurrence.clashesFor
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.clashes.Clash
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.clashes.ClashOrigin
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.clashes.ClashPersonIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.clashes.ClashRequest
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.clashes.ClashResponse
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.clashes.ClashSource
import uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.clashes.PersonClashes

@Transactional(readOnly = true)
@Service
class RetrieveClashes(private val occurrenceRepository: TemporaryAbsenceOccurrenceRepository) {
  fun retrieve(request: ClashRequest): ClashResponse = occurrenceRepository.findAll(clashesFor(request.personIdentifiers.map { it.value }.toSet(), request.ranges))
    .groupBy { it.person.identifier }
    .map { (k, v) -> PersonClashes(k.asPersonIdentifier(), v.map { it.clash() }) }
    .let { ClashResponse(ORIGIN, it) }

  private fun TemporaryAbsenceOccurrence.clash() = Clash(start, end, description(), OUTSIDE, additionalInfo())

  private fun TemporaryAbsenceOccurrence.description() = Clash.Description(hierarchyDescription(reasonPath), shortDescription())

  private fun TemporaryAbsenceOccurrence.additionalInfo() = Clash.AdditionalInformation(absenceReason.code)

  private fun String.asPersonIdentifier() = ClashPersonIdentifier(ClashPersonIdentifier.Type.PRISON_NUMBER, this)

  companion object {
    const val PRODUCT_ID = "DPS125"
    val ORIGIN: ClashOrigin = ClashOrigin(ClashSource(PRODUCT_ID, "Schedule a temporary absence"))
    val OUTSIDE = Clash.Location("Outside")
  }
}
