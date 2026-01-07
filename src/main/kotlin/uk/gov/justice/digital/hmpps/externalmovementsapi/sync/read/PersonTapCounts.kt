package uk.gov.justice.digital.hmpps.externalmovementsapi.sync.read

data class PersonTapCounts(
  val authorisations: PersonAuthorisationCount,
  val occurrences: PersonOccurrenceCount,
  val movements: PersonMovementsCount,
)

data class PersonAuthorisationCount(val count: Int)
data class PersonOccurrenceCount(val count: Int)
data class PersonMovementsCount(val scheduled: MovementInOutCount, val unscheduled: MovementInOutCount)
data class MovementInOutCount(val outCount: Int, val inCount: Int)
