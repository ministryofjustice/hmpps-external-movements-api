package uk.gov.justice.digital.hmpps.externalmovementsapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ExternalMovementsApi

fun main(args: Array<String>) {
  runApplication<ExternalMovementsApi>(*args)
}
