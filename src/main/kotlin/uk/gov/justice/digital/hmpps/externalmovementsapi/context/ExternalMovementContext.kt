package uk.gov.justice.digital.hmpps.externalmovementsapi.context

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.externalmovementsapi.context.ExternalMovementContext.Companion.SYSTEM_USERNAME
import java.time.LocalDateTime

data class ExternalMovementContext(
  val username: String,
  val requestAt: LocalDateTime = LocalDateTime.now(),
  val source: DataSource = DataSource.DPS,
) {
  companion object {
    const val SYSTEM_USERNAME = "SYS"

    fun get(): ExternalMovementContext = ExternalMovementContextHolder.getContext()
    fun clear() {
      ExternalMovementContextHolder.clearContext()
    }
  }
}

fun ExternalMovementContext.set() = apply { ExternalMovementContextHolder.setContext(this) }

@Component
class ExternalMovementContextHolder {
  companion object {
    private var context: ThreadLocal<ExternalMovementContext> =
      ThreadLocal.withInitial { ExternalMovementContext(SYSTEM_USERNAME) }

    internal fun getContext(): ExternalMovementContext = context.get()
    internal fun setContext(emc: ExternalMovementContext) {
      context.set(emc)
    }

    internal fun clearContext() {
      context.remove()
    }
  }
}
