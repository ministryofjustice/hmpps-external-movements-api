package uk.gov.justice.digital.hmpps.externalmovementsapi.integration

import org.junit.jupiter.api.Test

class InitialiseDatabase : IntegrationTest() {

  @Test
  fun `initialises database`() {
    println("Database has been initialised by IntegrationTestBase")
  }
}
