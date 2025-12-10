package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceRescheduled
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.StartAndEnd
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.ValidStartAndEnd
import java.time.LocalDateTime
import kotlin.reflect.KClass

@ValidReschedule
@ValidStartAndEnd
data class RescheduleOccurrence(
  override val start: LocalDateTime?,
  override val end: LocalDateTime?,
  override val reason: String? = null,
) : OccurrenceAction,
  StartAndEnd<LocalDateTime> {
  override fun domainEvent(tao: TemporaryAbsenceOccurrence): DomainEvent<*> = TemporaryAbsenceRescheduled(tao.authorisation.person.identifier, tao.id)
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [RescheduleOccurrenceValidator::class])
annotation class ValidReschedule(
  val message: String = DEFAULT_MESSAGE,
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Any>> = [],
) {
  companion object {
    const val DEFAULT_MESSAGE = "Either start or end must be provided."
  }
}

class RescheduleOccurrenceValidator : ConstraintValidator<ValidReschedule, RescheduleOccurrence> {
  override fun isValid(rescheduleOccurrence: RescheduleOccurrence, context: ConstraintValidatorContext): Boolean = rescheduleOccurrence.start != null || rescheduleOccurrence.end != null
}
