package uk.gov.justice.digital.hmpps.externalmovementsapi.sync

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(name = "SyncAtAndBy")
data class AtAndBy(val at: LocalDateTime, val by: String)
