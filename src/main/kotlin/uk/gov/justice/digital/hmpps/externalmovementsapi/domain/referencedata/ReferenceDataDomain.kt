package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AccompaniedBy
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.AuthorisationStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.OccurrenceStatus
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.Transport
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReason
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceReasonCategory
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceSubType
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceType
import uk.gov.justice.digital.hmpps.externalmovementsapi.exception.NotFoundException
import kotlin.reflect.KClass

@Immutable
@Entity
@Table(name = "reference_data_domain")
class ReferenceDataDomain(
  @Id
  @Enumerated(EnumType.STRING)
  val code: Code,
  val description: String,
) {
  enum class Code(val clazz: KClass<out ReferenceData>) {
    ABSENCE_TYPE(AbsenceType::class),
    ABSENCE_SUB_TYPE(AbsenceSubType::class),
    ABSENCE_REASON_CATEGORY(AbsenceReasonCategory::class),
    ABSENCE_REASON(AbsenceReason::class),

    ACCOMPANIED_BY(AccompaniedBy::class),
    TRANSPORT(Transport::class),
    TAP_AUTHORISATION_STATUS(AuthorisationStatus::class),
    TAP_OCCURRENCE_STATUS(OccurrenceStatus::class),
    ;

    companion object {
      fun of(domain: String): Code = entries.firstOrNull {
        it.name.lowercase().replace("_", "") == domain.lowercase().replace("[_|-]".toRegex(), "")
      } ?: throw NotFoundException("Reference data domain not found")
    }
  }
}

interface ReferenceDataDomainRepository : JpaRepository<ReferenceDataDomain, ReferenceDataDomain.Code>

fun ReferenceDataDomainRepository.getDomain(code: ReferenceDataDomain.Code): ReferenceDataDomain = findByIdOrNull(code) ?: throw NotFoundException("Reference data domain not found")
