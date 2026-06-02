package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.sync.internal

import com.microsoft.applicationinsights.TelemetryClient

const val UNEXPECTED_SYNC_EVENT = "UnexpectedSyncEvent"

sealed interface SyncRequestStatus {
  val action: ActionType
  val entity: EntityType

  data class NotFound(override val action: ActionType, override val entity: EntityType) : SyncRequestStatus
  data class Actionable(override val action: ActionType, override val entity: EntityType) : SyncRequestStatus
  data class NotActionable(override val action: ActionType, override val entity: EntityType, val reason: String) : SyncRequestStatus
}

enum class EntityType {
  PLAN,
  ABSENCE,
  MOVEMENT,
}

enum class ActionType {
  CREATE,
  UPDATE,
  DELETE,
}

fun SyncRequestStatus.response(telemetryClient: TelemetryClient) {
  when (this) {
    is SyncRequestStatus.NotActionable -> {
      telemetryClient.trackEvent(
        UNEXPECTED_SYNC_EVENT,
        mapOf("action" to "$action", "entity" to "$entity", "reason" to reason),
        mapOf(),
      )
    }

    is SyncRequestStatus.Actionable, is SyncRequestStatus.NotFound -> {
      telemetryClient.trackEvent(UNEXPECTED_SYNC_EVENT, mapOf("action" to "$action", "entity" to "$entity"), mapOf())
    }
  }
}
