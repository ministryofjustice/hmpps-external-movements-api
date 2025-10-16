package uk.gov.justice.digital.hmpps.externalmovementsapi.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.validation.constraints.Size
import org.hibernate.envers.Audited

@Audited
@Embeddable
data class Location(
  @Size(max = 36)
  @Column(name = "location_id", length = 36)
  val identifier: String?,
  @Column(name = "location_description")
  val description: String?,
  @Column(name = "location_premise")
  val premise: String?,
  @Column(name = "location_street")
  val street: String?,
  @Column(name = "location_area")
  val area: String?,
  @Column(name = "location_city")
  val city: String?,
  @Column(name = "location_county")
  val county: String?,
  @Column(name = "location_country")
  val country: String?,
  @Column(name = "location_postcode")
  val postcode: String?,
)
