package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation

import com.fasterxml.jackson.annotation.JsonProperty
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
  val absencesPerDay: Int? = null,
) : AuthorisationSchedule {
  override val type = AuthorisationSchedule.Type.WEEKLY
}

data class BiWeeklyPattern(val weekA: List<WeekDayPattern> = listOf(), val weekB: List<WeekDayPattern> = listOf())

data class BiWeeklySchedule(
  @JsonProperty("biweeklyPattern")
  val biWeeklyPattern: BiWeeklyPattern = BiWeeklyPattern(),
  val absencesPerDay: Int? = null,
) : AuthorisationSchedule {
  override val type = AuthorisationSchedule.Type.BIWEEKLY
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  value = [
    Type(value = DayShiftPattern::class, name = "DAY"),
    Type(value = NightShiftPattern::class, name = "NIGHT"),
    Type(value = RestShiftPattern::class, name = "REST"),
  ],
)
interface ShiftPattern {
  val type: ShiftPattern.Type
  val count: Int

  enum class Type {
    DAY,
    NIGHT,
    REST,
  }
}

data class DayShiftPattern(
  override val count: Int,
  val startTime: LocalTime,
  val returnTime: LocalTime,
) : ShiftPattern {
  override val type = ShiftPattern.Type.DAY
}

data class NightShiftPattern(
  override val count: Int,
  val startTime: LocalTime,
  val returnTime: LocalTime,
) : ShiftPattern {
  override val type = ShiftPattern.Type.NIGHT
}

data class RestShiftPattern(
  override val count: Int,
) : ShiftPattern {
  override val type = ShiftPattern.Type.REST
}

data class ShiftSchedule(
  val shiftPattern: List<ShiftPattern> = listOf(),
) : AuthorisationSchedule {
  override val type = AuthorisationSchedule.Type.SHIFT
}
