package uk.gov.justice.digital.hmpps.externalmovementsapi.domain.tap

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.envers.Audited
import org.hibernate.type.SqlTypes
import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.externalmovementsapi.model.location.Location
import java.util.SequencedSet

@Entity
@Audited
@Table(schema = "tap", name = "prison_tap_locations")
final class PrisonTapLocations(
  @Id
  @Column(name = "prison_code")
  override val prisonCode: String,
  @Version
  val version: Int = 0,
  @JdbcTypeCode(SqlTypes.JSON)
  val locations: SequencedSet<Location>,
) : PrisonRelated

interface PrisonTapLocationsRepository : JpaRepository<PrisonTapLocations, String>
