package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.person

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import jakarta.validation.constraints.Size
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

@Entity
@Table
class PersonSummary(
  firstName: String,
  lastName: String,
  dateOfBirth: LocalDate,
  cellLocation: String?,
  @Id
  @Size(max = 10)
  @Column(name = "person_identifier")
  val identifier: String,
) {
  var firstName: String = firstName
    private set
  var lastName: String = lastName
    private set
  var dateOfBirth: LocalDate = dateOfBirth
    private set
  var cellLocation: String? = cellLocation
    private set

  @Version
  val version: Int? = null

  fun update(firstName: String, lastName: String, dateOfBirth: LocalDate, cellLocation: String?) = apply {
    if (this.firstName != firstName || this.lastName != lastName || !this.dateOfBirth.isEqual(dateOfBirth) || this.cellLocation != cellLocation) {
      this.firstName = firstName
      this.lastName = lastName
      this.dateOfBirth = dateOfBirth
      this.cellLocation = cellLocation
    }
  }

  companion object {
    val IDENTIFIER: String = PersonSummary::identifier.name
    val FIRST_NAME: String = PersonSummary::firstName.name
    val LAST_NAME: String = PersonSummary::lastName.name
  }
}

interface PersonSummaryRepository : JpaRepository<PersonSummary, String>
