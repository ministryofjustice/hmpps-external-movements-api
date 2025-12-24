package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.cache

import jakarta.persistence.Query
import jakarta.persistence.TypedQuery

const val CACHEABLE_HINT_KEY = "org.hibernate.cacheable"
fun Query.cacheable(): Query = setHint(CACHEABLE_HINT_KEY, true)
fun <T> TypedQuery<T>.cacheable(): TypedQuery<T> = setHint(CACHEABLE_HINT_KEY, true)
