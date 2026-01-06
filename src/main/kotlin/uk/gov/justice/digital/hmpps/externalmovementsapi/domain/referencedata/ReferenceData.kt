package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.cache.cacheable
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.ReferenceDataPaths
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata.absencereason.AbsenceCategorisationLinkRepository
import java.util.UUID
import kotlin.reflect.KClass

interface ReferenceData {
  val code: String
  val description: String
  val sequenceNumber: Int
  val active: Boolean
  val id: UUID

  companion object {
    val CODE = ReferenceData::code.name
    val SEQUENCE_NUMBER = ReferenceData::sequenceNumber.name
  }
}

@Transactional(readOnly = true)
@Repository
class ReferenceDataRepository(
  private val entityManager: EntityManager,
  private val linkRepository: AbsenceCategorisationLinkRepository,
) {

  fun findAll(): List<ReferenceData> = entityManager.createQuery(
    "from uk.gov.justice.digital.hmpps.externalmovementsapi.domain.referencedata.ReferenceData",
  ).cacheable().resultList.filterIsInstance<ReferenceData>()

  fun findAllByType(clazz: KClass<out ReferenceData>): List<ReferenceData> =
    entityManager.createQuery("from ${clazz.qualifiedName}", clazz.java).cacheable().resultList

  fun rdProvider(): (KClass<out ReferenceData>, String) -> ReferenceData {
    val allRd = findAll().associateBy { it::class to it.code }
    return { domain: KClass<out ReferenceData>, code: String -> requireNotNull(allRd[domain to code]) }
  }

  fun referenceDataFor(required: ReferenceDataRequired): ReferenceDataPaths = with(required) {
    val allRd = findAll()
    val rdLinks =
      linkRepository.findAll().groupBy({ it.id2 to it.domain1 }, { link -> allRd.first { it.id == link.id1 } })
    val findLinkedFrom: (UUID, ReferenceDataDomain.Code) -> List<ReferenceData> =
      { id: UUID, domainCode: ReferenceDataDomain.Code -> rdLinks[id to domainCode] ?: emptyList() }
    return ReferenceDataPaths(
      allRd.filter { rd ->
        rd::class to rd.code in requiredReferenceData().map { it.domain.clazz to it.code }
      },
      findLinkedFrom,
    )
  }
}
