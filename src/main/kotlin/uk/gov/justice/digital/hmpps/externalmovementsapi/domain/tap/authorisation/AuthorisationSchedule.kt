package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalTime

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  value = [
    Type(value = SingleSchedule::class, name = "SINGLE"),
    Type(value = FreeFormSchedule::class, name = "FREEFORM"),
    Type(value = WeeklySchedule::class, name = "WEEKLY"),
    Type(value = BiWeeklySchedule::class, name = "BIWEEKLY"),
    Type(value = ShiftSchedule::class, name = "SHIFT"),
  ],
)
sealed interface AuthorisationSchedule {
  val type: Type

  enum class Type {
    SINGLE,
    FREEFORM,
    WEEKLY,
    BIWEEKLY,
    SHIFT,
  }
}

data class SingleSchedule(
  val startTime: LocalTime,
  val returnTime: LocalTime,
) : AuthorisationSchedule {
  override val type = AuthorisationSchedule.Type.SINGLE
}

data object FreeFormSchedule : AuthorisationSchedule {
  override val type = AuthorisationSchedule.Type.FREEFORM
}

data class WeekDayPattern(val day: Int, val overnight: Boolean, val startTime: LocalTime, val returnTime: LocalTime)

data class WeeklySchedule(
  val weeklyPattern: List<WeekDayPattern> = listOf(),
  val absencesPerDay: Int?,
) : AuthorisationSchedule {
  override val type = AuthorisationSchedule.Type.WEEKLY
}

data class BiWeeklyPattern(val weekA: List<WeekDayPattern>, val weekB: List<WeekDayPattern>)

data class BiWeeklySchedule(
  val biWeeklyPattern: BiWeeklyPattern,
  val absencesPerDay: Int?,
) : AuthorisationSchedule {
  override val type = AuthorisationSchedule.Type.BIWEEKLY
}

data class ShiftPattern(val type: ShiftPattern.Type, val startTime: LocalTime?, val returnTime: LocalTime?) {
  enum class Type {
    DAY,
    NIGHT,
    REST,
  }
}

data class ShiftSchedule(
  val shiftPattern: List<ShiftPattern> = listOf(),
) : AuthorisationSchedule {
  override val type = AuthorisationSchedule.Type.SHIFT
}
