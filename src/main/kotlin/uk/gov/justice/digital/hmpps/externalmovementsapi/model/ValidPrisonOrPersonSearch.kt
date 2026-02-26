package uk.gov.justice.digital.hmpps.externalmovementsapi.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.integration.prisonersearch.Prisoner
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import kotlin.reflect.KClass

interface PersonIdentifierDateRange<T : Temporal> : StartAndEnd<T> {
  val query: String?

  @JsonIgnore
  fun isPersonIdentifier(): Boolean = query?.matches(Prisoner.PATTERN.toRegex()) == true
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [PersonIdentifierDateRangeValidator::class])
annotation class ValidPersonIdentifierOrDateRange(
  val message: String = DEFAULT_MESSAGE,
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Any>> = [],
) {
  companion object {
    const val DEFAULT_MESSAGE = "A valid person identifier is required or valid start and end."
  }
}

class PersonIdentifierDateRangeValidator : ConstraintValidator<ValidPersonIdentifierOrDateRange, PersonIdentifierDateRange<*>> {
  override fun isValid(request: PersonIdentifierDateRange<*>, context: ConstraintValidatorContext): Boolean = with(request) {
    return if (request.isPersonIdentifier()) {
      true
    } else {
      start != null && end != null && ChronoUnit.DAYS.between(start, end) <= 31
    }
  }
}
