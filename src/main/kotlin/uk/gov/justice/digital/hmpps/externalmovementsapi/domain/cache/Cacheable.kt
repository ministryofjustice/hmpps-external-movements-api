package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.cache

import jakarta.persistence.Query
import jakarta.persistence.TypedQuery
import org.hibernate.jpa.AvailableHints

fun Query.cacheable(): Query = setHint(AvailableHints.HINT_CACHEABLE, true)

fun <T> TypedQuery<T>.cacheable(): TypedQuery<T> = setHint(AvailableHints.HINT_CACHEABLE, true)
