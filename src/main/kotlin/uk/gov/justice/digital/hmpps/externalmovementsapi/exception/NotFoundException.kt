package uk.gov.justice.digital.hmpps.externalmovementsapi.exception

data class NotFoundException(override val message: String) : RuntimeException(message)
