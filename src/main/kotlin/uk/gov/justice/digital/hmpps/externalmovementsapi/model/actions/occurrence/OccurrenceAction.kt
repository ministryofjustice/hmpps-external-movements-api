package uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.occurrence

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.absence.occurrence.TemporaryAbsenceOccurrence
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.DomainEvent
import uk.gov.justice.digital.hmpps.externalmovementsapi.events.TemporaryAbsenceRescheduled
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.actions.Action
import java.time.LocalDateTime
import kotlin.reflect.KClass

sealed interface OccurrenceAction : Action {
  fun domainEvent(tao: TemporaryAbsenceOccurrence): DomainEvent<*>
}

@ValidReschedule
data class RescheduleOccurrence(
  val releaseAt: LocalDateTime?,
  val returnBy: LocalDateTime?,
  override val reason: String?,
) : OccurrenceAction {
  override fun domainEvent(tao: TemporaryAbsenceOccurrence): DomainEvent<*> = TemporaryAbsenceRescheduled(tao.personIdentifier, tao.id)
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
    const val DEFAULT_MESSAGE = "Either release or return date must be provided."
  }
}

class RescheduleOccurrenceValidator : ConstraintValidator<ValidReschedule, RescheduleOccurrence> {
  override fun isValid(rescheduleOccurrence: RescheduleOccurrence, context: ConstraintValidatorContext): Boolean = rescheduleOccurrence.releaseAt != null || rescheduleOccurrence.returnBy != null
}
