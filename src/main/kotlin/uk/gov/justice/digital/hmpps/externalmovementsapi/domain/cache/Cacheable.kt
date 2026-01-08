package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.cache

import jakarta.persistence.CacheRetrieveMode
import jakarta.persistence.Query
import jakarta.persistence.TypedQuery
import org.hibernate.CacheMode
import org.hibernate.jpa.AvailableHints

fun Query.cacheable(): Query = setHint(AvailableHints.HINT_CACHEABLE, true)
  .setHint(AvailableHints.HINT_SPEC_CACHE_RETRIEVE_MODE, CacheRetrieveMode.USE)
  .setHint(AvailableHints.HINT_CACHE_MODE, CacheMode.GET)

fun <T> TypedQuery<T>.cacheable(): TypedQuery<T> = setHint(AvailableHints.HINT_CACHEABLE, true)
  .setHint(AvailableHints.HINT_SPEC_CACHE_RETRIEVE_MODE, CacheRetrieveMode.USE)
  .setHint(AvailableHints.HINT_CACHE_MODE, CacheMode.GET)
