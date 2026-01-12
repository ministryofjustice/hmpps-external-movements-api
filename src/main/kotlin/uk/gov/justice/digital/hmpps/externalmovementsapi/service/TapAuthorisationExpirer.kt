package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Conditional
import org.springframework.core.env.getProperty
import org.springframework.core.type.AnnotatedTypeMetadata
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatusRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.getByCode
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ExpireAuthorisation

@Transactional
@Service
class AuthorisationExpirer(
  private val authorisationStatusRepository: AuthorisationStatusRepository,
  private val authorisationRepository: TemporaryAbsenceAuthorisationRepository,
) {
  fun expireUnapprovedAuthorisations() {
    authorisationRepository.findRecentlyExpired().takeIf { it.isNotEmpty() }
      ?.also {
        val expired = authorisationStatusRepository.getByCode(AuthorisationStatus.Code.EXPIRED.name)
        it.forEach { taa -> taa.expire(ExpireAuthorisation()) { _, _ -> expired } }
      }
  }
}

@Conditional(PollExpiredAuthorisationCondition::class)
@Service
class AuthorisationExpiringPoller(private val authorisationExpirer: AuthorisationExpirer) {
  @Scheduled(cron = $$"${service.authorisation-expiration.cron}")
  fun recalculatePastOccurrences() = try {
    authorisationExpirer.expireUnapprovedAuthorisations()
  } finally {
    ExternalMovementContext.clear()
  }
}

class PollExpiredAuthorisationCondition : Condition {
  override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean = context.environment.getProperty<String>("service.authorisation-expiration.cron", "").isNotBlank()
}
