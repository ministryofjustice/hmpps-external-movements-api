package uk.gov.justice.digital.hmpps.externalmovementsapi.model.referencedata

data class ReferenceDataResponse(val domain: CodedDescription, val items: List<CodedDescription>)
