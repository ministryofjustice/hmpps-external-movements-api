package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceDataDomain.Code

@Immutable
@Entity
@IdClass(PrisonAbsenceCategorisationId::class)
@Table(schema = "tap", name = "prison_absence_categorisation")
class PrisonAbsenceCategorisation(
  @Id
  val prisonCode: String,
  @Id
  @Enumerated(EnumType.STRING)
  val domainCode: Code,
  @Id
  val code: String,
  val description: String,
  val active: Boolean,
  val sequenceNumber: Int,
)

data class PrisonAbsenceCategorisationId(
  val prisonCode: String? = null,
  val domainCode: Code? = null,
  val code: String? = null,
)

interface PrisonAbsenceCategorisationRepository : JpaRepository<PrisonAbsenceCategorisation, PrisonAbsenceCategorisationId> {
  fun findByPrisonCode(prisonCode: String): List<PrisonAbsenceCategorisation>
}
