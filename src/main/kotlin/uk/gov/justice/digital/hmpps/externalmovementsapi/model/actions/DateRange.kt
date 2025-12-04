package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import java.time.LocalDate
import kotlin.reflect.KClass

interface DateRange {
  val from: LocalDate
  val to: LocalDate
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [DateRangeValidator::class])
annotation class ValidDateRange(
  val message: String = DEFAULT_MESSAGE,
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Any>> = [],
) {
  companion object {
    const val DEFAULT_MESSAGE = "The authorisation date range must not be more than 6 months"
  }
}

class DateRangeValidator : ConstraintValidator<ValidDateRange, DateRange> {
  override fun isValid(request: DateRange, context: ConstraintValidatorContext): Boolean = !request.from.plusMonths(6).isBefore(request.to)
}
