package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.events

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.movement.TemporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.PersonReference
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.PrisonerMerged
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.PrisonerMergedInformation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.personIdentifier
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.DataGenerator.prisonCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceMovementOperations.Companion.temporaryAbsenceMovement
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceOccurrenceOperations.Companion.temporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.PrisonerMergedHandler

class PrisonerMergedIntTest(
  @Autowired private val mergedHandler: PrisonerMergedHandler,
  @Autowired private val taaOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val taoOperations: TempAbsenceOccurrenceOperations,
  @Autowired private val tamOperations: TempAbsenceMovementOperations,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by taaOperations,
  TempAbsenceOccurrenceOperations by taoOperations,
  TempAbsenceMovementOperations by tamOperations {

  @Test
  fun `prisoner merged when no data for prisoner`() {
    val prisonCode = prisonCode()
    val fromPi = personIdentifier()
    val toPi = personIdentifier()
    prisonerSearch.getPrisoners(prisonCode, setOf(toPi))

    mergedHandler.handle(prisonerMergedEvent(fromPi, toPi))
  }

  @Test
  fun `prisoner merge event successful`() {
    val prisonCode = prisonCode()
    val fromPi = personIdentifier()
    val toPi = personIdentifier()
    prisonerSearch.getPrisoners(prisonCode, setOf(toPi))
    val auth = givenTemporaryAbsenceAuthorisation(temporaryAbsenceAuthorisation(prisonCode, fromPi))
    val occ = givenTemporaryAbsenceOccurrence(temporaryAbsenceOccurrence(auth))
    val sm = givenTemporaryAbsenceMovement(
      temporaryAbsenceMovement(TemporaryAbsenceMovement.Direction.OUT, fromPi, occ),
    )
    val um = givenTemporaryAbsenceMovement(
      temporaryAbsenceMovement(TemporaryAbsenceMovement.Direction.IN, fromPi),
    )

    mergedHandler.handle(prisonerMergedEvent(fromPi, toPi))

    val context = ExternalMovementContext.get().copy(reason = PrisonerMerged.DESCRIPTION)
    with(requireNotNull(findTemporaryAbsenceAuthorisation(auth.id))) {
      assertThat(person.identifier).isEqualTo(toPi)
      verifyAudit(
        this,
        RevisionType.MOD,
        setOf(
          TemporaryAbsenceAuthorisation::class.simpleName!!,
          TemporaryAbsenceMovement::class.simpleName!!,
        ),
        context,
      )
    }
    with(requireNotNull(findTemporaryAbsenceMovement(sm.id))) {
      assertThat(personIdentifier).isEqualTo(toPi)
      verifyAudit(
        this,
        RevisionType.MOD,
        setOf(
          TemporaryAbsenceAuthorisation::class.simpleName!!,
          TemporaryAbsenceMovement::class.simpleName!!,
        ),
        context,
      )
    }
    with(requireNotNull(findTemporaryAbsenceMovement(um.id))) {
      assertThat(personIdentifier).isEqualTo(toPi)
      verifyAudit(
        this,
        RevisionType.MOD,
        setOf(
          TemporaryAbsenceAuthorisation::class.simpleName!!,
          TemporaryAbsenceMovement::class.simpleName!!,
        ),
        context,
      )
    }
  }

  private fun prisonerMergedEvent(from: String, to: String): PrisonerMerged = PrisonerMerged(
    PrisonerMergedInformation(from, to),
    PersonReference.withIdentifier(to),
  )
}
