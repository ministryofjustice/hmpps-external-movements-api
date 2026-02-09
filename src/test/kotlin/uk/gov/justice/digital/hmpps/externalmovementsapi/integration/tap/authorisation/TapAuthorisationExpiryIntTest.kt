package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.tap.authorisation

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus.Code.EXPIRED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus.Code.PENDING
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.IntegrationTest
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.config.TempAbsenceAuthorisationOperations.Companion.temporaryAbsenceAuthorisation
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.AuthorisationExpirer
import java.time.LocalDate

class TapAuthorisationExpiryIntTest(
  @Autowired private val taaOperations: TempAbsenceAuthorisationOperations,
  @Autowired private val authorisationExpirer: AuthorisationExpirer,
) : IntegrationTest(),
  TempAbsenceAuthorisationOperations by taaOperations {
  @Test
  fun `authorisation expired when date range passed`() {
    val yesterday = LocalDate.now().minusDays(1)
    val today = LocalDate.now()
    val toExpire = (0..20).map {
      givenTemporaryAbsenceAuthorisation(
        temporaryAbsenceAuthorisation(
          status = PENDING,
          start = yesterday.minusDays(1),
          end = yesterday,
        ),
      )
    }
    val noExpire = listOf(
      givenTemporaryAbsenceAuthorisation(
        temporaryAbsenceAuthorisation(
          status = PENDING,
          start = yesterday,
          end = today,
        ),
      ),
      givenTemporaryAbsenceAuthorisation(
        temporaryAbsenceAuthorisation(
          status = PENDING,
          start = today,
          end = today.plusDays(1),
        ),
      ),
    )

    authorisationExpirer.expireUnapprovedAuthorisations()

    toExpire.forEach {
      val expired = requireNotNull(findTemporaryAbsenceAuthorisation(it.id))
      assertThat(expired.status.code).isEqualTo(EXPIRED.name)
      verifyAudit(
        expired,
        RevisionType.MOD,
        setOf(TemporaryAbsenceAuthorisation::class.simpleName!!, HmppsDomainEvent::class.simpleName!!),
      )
    }

    noExpire.forEach {
      requireNotNull(findTemporaryAbsenceAuthorisation(it.id))
      assertThat(it.status.code).isEqualTo(PENDING.name)
    }
  }
}
