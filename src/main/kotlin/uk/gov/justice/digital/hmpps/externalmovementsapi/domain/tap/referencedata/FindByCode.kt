package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.referencedata

fun interface FindByCode<T : DomainLinkedReferenceData> {
  fun findByCode(code: String): T?
}
