package uk.gov.justice.digital.hmpps.externalmovementsapi.model

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Valid
import jakarta.validation.constraints.FutureOrPresent
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import java.time.LocalDateTime
import kotlin.reflect.KClass

@ValidReleaseAndReturn
data class CreateOccurrenceRequest(
  @FutureOrPresent(message = "Absence cannot be scheduled in the past.")
  val releaseAt: LocalDateTime,
  val returnBy: LocalDateTime,
  @Valid
  val location: Location,
  val notes: String?,
)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [ReleaseAndReturnValidator::class])
annotation class ValidReleaseAndReturn(
  val message: String = DEFAULT_MESSAGE,
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Any>> = [],
) {
  companion object {
    const val DEFAULT_MESSAGE = "Return by must be after release at."
  }
}

class ReleaseAndReturnValidator : ConstraintValidator<ValidReleaseAndReturn, CreateOccurrenceRequest> {
  override fun isValid(request: CreateOccurrenceRequest, context: ConstraintValidatorContext): Boolean = !request.returnBy.isBefore(request.releaseAt)
}
