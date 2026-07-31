package uk.gov.justice.digital.hmpps.externalmovementsapi.tap.model.clashes

data class ClashPersonIdentifier(val type: Type, val value: String) {
  enum class Type {
    PRISON_NUMBER,
  }
}
