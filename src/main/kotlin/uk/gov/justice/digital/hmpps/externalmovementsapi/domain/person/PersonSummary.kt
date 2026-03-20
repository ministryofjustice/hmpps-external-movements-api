package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import jakarta.validation.constraints.Size
import org.springframework.data.jpa.repository.JpaRepository

@Entity
@Table(name = "person_summary")
final class PersonSummary(
  firstName: String,
  lastName: String,
  prisonCode: String?,
  cellLocation: String?,
  @Id
  @Size(max = 10)
  @Column(name = "person_identifier")
  val identifier: String,
) {
  @Column(name = "first_name")
  var firstName: String = firstName
    private set

  @Column(name = "last_name")
  var lastName: String = lastName
    private set

  @Column(name = "prison_code")
  var prisonCode: String? = prisonCode
    private set

  @Column(name = "cell_location")
  var cellLocation: String? = cellLocation
    private set

  @Version
  val version: Int? = null

  fun update(firstName: String, lastName: String, prisonCode: String?, cellLocation: String?) = apply {
    if (this.firstName != firstName || this.lastName != lastName || this.prisonCode != prisonCode || this.cellLocation != cellLocation) {
      this.firstName = firstName
      this.lastName = lastName
      this.prisonCode = prisonCode
      this.cellLocation = cellLocation
    }
  }

  companion object {
    val IDENTIFIER: String = PersonSummary::identifier.name
    val FIRST_NAME: String = PersonSummary::firstName.name
    val LAST_NAME: String = PersonSummary::lastName.name
    val PRISON_CODE: String = PersonSummary::prisonCode.name
  }
}

interface PersonSummaryRepository : JpaRepository<PersonSummary, String>
