package uk.gov.justice.digital.hmpps.externalmovementsapi.model.integration

data class IntegrationResponse<T>(
  val data: T,
  val previousUrl: String?,
  val nextUrl: String?,
)

data class IntegrationResponses<T>(
  val data: List<IntegrationResponse<T>>,
  val previousUrl: String?,
)
