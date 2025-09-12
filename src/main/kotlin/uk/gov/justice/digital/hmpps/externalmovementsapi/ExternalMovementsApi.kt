package uk.gov.justice.digital.hmpps.externalmovementsapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import uk.gov.justice.digital.hmpps.externalmovementsapi.config.ServiceConfig

@EnableConfigurationProperties(ServiceConfig::class)
@SpringBootApplication
class ExternalMovementsApi

fun main(args: Array<String>) {
  runApplication<ExternalMovementsApi>(*args)
}
