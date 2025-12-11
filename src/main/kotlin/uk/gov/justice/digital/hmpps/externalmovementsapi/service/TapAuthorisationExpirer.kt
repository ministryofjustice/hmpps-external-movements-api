package uk.gov.justice.digital.hmpps.externalmovementsapi.service

import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Conditional
import org.springframework.core.env.getProperty
import org.springframework.core.type.AnnotatedTypeMetadata
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.authorisation.TemporaryAbsenceAuthorisationRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code.TAP_AUTHORISATION_STATUS
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.TapAuthorisationStatus.Code.EXPIRED
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.getByKey
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.of
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.authorisation.ExpireAuthorisation

@Transactional
@Service
class AuthorisationExpirer(
  private val referenceDataRepository: ReferenceDataRepository,
  private val authorisationRepository: TemporaryAbsenceAuthorisationRepository,
) {
  fun expireUnapprovedAuthorisations() {
    authorisationRepository.findRecentlyExpired().takeIf { it.isNotEmpty() }
      ?.also {
        val expired = referenceDataRepository.getByKey(TAP_AUTHORISATION_STATUS of EXPIRED.name)
        it.forEach { taa -> taa.expire(ExpireAuthorisation()) { _, _ -> expired } }
      }
  }
}

@Conditional(PollExpiredAuthorisationCondition::class)
@Service
class AuthorisationExpiringPoller(private val authorisationExpirer: AuthorisationExpirer) {
  @Scheduled(cron = $$"${service.authorisation-expiration.cron}")
  fun recalculatePastOccurrences() {
    authorisationExpirer.expireUnapprovedAuthorisations()
  }
}

class PollExpiredAuthorisationCondition : Condition {
  override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean = context.environment.getProperty<String>("service.authorisation-expiration.cron", "").isNotBlank()
}
