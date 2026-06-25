package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata

import jakarta.persistence.Column
import jakarta.persistence.EntityManager
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.cache.cacheable
import java.util.UUID
import kotlin.reflect.KClass

@MappedSuperclass
abstract class ReferenceData(
  val code: String,
  val description: String,
  @Column(name = "sequence_number")
  val sequenceNumber: Int,
  val active: Boolean,
  @Id
  val id: UUID,
) {
  companion object {
    val CODE = ReferenceData::code.name
    val SEQUENCE_NUMBER = ReferenceData::sequenceNumber.name
  }
}

@Transactional(readOnly = true)
@Repository
class ReferenceDataRepository(
  private val entityManager: EntityManager,
) {
  fun findAll(): List<ReferenceData> = entityManager.createQuery(
    "from uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData",
  ).cacheable().resultList.filterIsInstance<ReferenceData>()

  fun findAllByType(clazz: KClass<out ReferenceData>): List<ReferenceData> = entityManager.createQuery("from ${clazz.qualifiedName}", clazz.java).cacheable().resultList

  fun rdProvider(): (KClass<out ReferenceData>, String) -> ReferenceData {
    val allRd = findAll().associateBy { it::class to it.code }
    return { domain: KClass<out ReferenceData>, code: String -> requireNotNull(allRd[domain to code]) }
  }
}
