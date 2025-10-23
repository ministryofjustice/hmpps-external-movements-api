package uk.gov.justice.digital.hmpps.externalmovementsapi.model.location

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [LocationValidator::class])
annotation class ValidLocation(
  val message: String = DEFAULT_MESSAGE,
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Any>> = [],
) {
  companion object {
    const val DEFAULT_MESSAGE = "Either a description or partial address must be specified."
  }
}

class LocationValidator : ConstraintValidator<ValidLocation, Location> {
  override fun isValid(location: Location, context: ConstraintValidatorContext): Boolean = with(location) {
    return !(description.isNullOrBlank() && (location.address?.isEmpty() ?: true))
  }
}
