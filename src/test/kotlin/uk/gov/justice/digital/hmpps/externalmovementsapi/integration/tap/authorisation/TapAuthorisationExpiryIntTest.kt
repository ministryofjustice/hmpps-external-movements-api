package uk.gov.justice.digital.hmpps.externalmovementsapi.integration.tap.authorisation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus
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
    val toExpire =
      givenTemporaryAbsenceAuthorisation(
        temporaryAbsenceAuthorisation(
          status = TapAuthorisationStatus.Code.PENDING,
          start = yesterday.minusDays(1),
          end = yesterday,
        ),
      )
    val noExpire = listOf(
      givenTemporaryAbsenceAuthorisation(
        temporaryAbsenceAuthorisation(
          status = TapAuthorisationStatus.Code.PENDING,
          start = yesterday,
          end = today,
        ),
      ),
      givenTemporaryAbsenceAuthorisation(
        temporaryAbsenceAuthorisation(
          status = TapAuthorisationStatus.Code.PENDING,
          start = today,
          end = today.plusDays(1),
        ),
      ),
    )

    authorisationExpirer.expireUnapprovedAuthorisations()

    val expired = requireNotNull(findTemporaryAbsenceAuthorisation(toExpire.id))
    assertThat(expired.status.code).isEqualTo(TapAuthorisationStatus.Code.EXPIRED.name)

    noExpire.forEach {
      requireNotNull(findTemporaryAbsenceAuthorisation(it.id))
      assertThat(it.status.code).isEqualTo(TapAuthorisationStatus.Code.PENDING.name)
    }
  }
}
