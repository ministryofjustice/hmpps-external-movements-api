package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap.authorisation

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalTime

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes(
  value = [
    Type(value = SingleSchedule::class, name = "SINGLE"),
    Type(value = FreeFormSchedule::class, name = "FREEFORM"),
    Type(value = WeeklySchedule::class, name = "WEEKLY"),
    Type(value = BiweeklySchedule::class, name = "BIWEEKLY"),
    Type(value = ShiftSchedule::class, name = "SHIFT"),
  ],
)
@Schema(
  description = "AuthorisationSchedule",
  discriminatorProperty = "type",
  discriminatorMapping = [
    DiscriminatorMapping(schema = SingleSchedule::class, value = "SINGLE"),
    DiscriminatorMapping(schema = FreeFormSchedule::class, value = "FREEFORM"),
    DiscriminatorMapping(schema = WeeklySchedule::class, value = "WEEKLY"),
    DiscriminatorMapping(schema = BiweeklySchedule::class, value = "BIWEEKLY"),
    DiscriminatorMapping(schema = ShiftSchedule::class, value = "SHIFT"),
  ],
)
sealed interface AuthorisationSchedule {
  val type: Type

  @Schema(name = "AuthorisationScheduleType", enumAsRef = true)
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
  override val type: AuthorisationSchedule.Type = AuthorisationSchedule.Type.SINGLE
}

data object FreeFormSchedule : AuthorisationSchedule {
  override val type: AuthorisationSchedule.Type = AuthorisationSchedule.Type.FREEFORM
}

data class WeekDayPattern(val day: Int, val overnight: Boolean, val startTime: LocalTime, val returnTime: LocalTime)

data class WeeklySchedule(
  val weeklyPattern: List<WeekDayPattern> = listOf(),
  val absencesPerDay: Int? = null,
) : AuthorisationSchedule {
  override val type: AuthorisationSchedule.Type = AuthorisationSchedule.Type.WEEKLY
}

data class BiweeklyPattern(val weekA: List<WeekDayPattern> = listOf(), val weekB: List<WeekDayPattern> = listOf())

data class BiweeklySchedule(
  val biweeklyPattern: BiweeklyPattern = BiweeklyPattern(),
  val absencesPerDay: Int? = null,
) : AuthorisationSchedule {
  override val type: AuthorisationSchedule.Type = AuthorisationSchedule.Type.BIWEEKLY
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes(
  value = [
    Type(value = DayShiftPattern::class, name = "DAY"),
    Type(value = NightShiftPattern::class, name = "NIGHT"),
    Type(value = RestShiftPattern::class, name = "REST"),
  ],
)
@Schema(
  description = "ShiftPattern",
  discriminatorProperty = "type",
  discriminatorMapping = [
    DiscriminatorMapping(schema = DayShiftPattern::class, value = "DAY"),
    DiscriminatorMapping(schema = NightShiftPattern::class, value = "NIGHT"),
    DiscriminatorMapping(schema = RestShiftPattern::class, value = "REST"),
  ],
)
interface ShiftPattern {
  val type: ShiftPattern.Type
  val count: Int

  @Schema(name = "ShiftPatternType", enumAsRef = true)
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
  override val type: ShiftPattern.Type = ShiftPattern.Type.DAY
}

data class NightShiftPattern(
  override val count: Int,
  val startTime: LocalTime,
  val returnTime: LocalTime,
) : ShiftPattern {
  override val type: ShiftPattern.Type = ShiftPattern.Type.NIGHT
}

data class RestShiftPattern(
  override val count: Int,
) : ShiftPattern {
  override val type: ShiftPattern.Type = ShiftPattern.Type.REST
}

data class ShiftSchedule(
  val shiftPattern: List<ShiftPattern> = listOf(),
) : AuthorisationSchedule {
  override val type: AuthorisationSchedule.Type = AuthorisationSchedule.Type.SHIFT
}
