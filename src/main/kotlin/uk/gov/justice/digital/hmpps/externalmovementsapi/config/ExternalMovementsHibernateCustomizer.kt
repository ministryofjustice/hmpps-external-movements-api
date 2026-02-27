package uk.gov.justice.digital.hmpps.externalmovementsapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.hibernate.cfg.AvailableSettings
import org.hibernate.type.format.jackson.JacksonJsonFormatMapper
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.externalmovementsapi.domain.interceptor.EntityInterceptor

@Configuration
class ExternalMovementsHibernateCustomizer(
  private val objectMapper: ObjectMapper,
  private val entityInterceptor: EntityInterceptor,
) : HibernatePropertiesCustomizer {
  override fun customize(hibernateProperties: MutableMap<String, Any>) {
    hibernateProperties[AvailableSettings.JSON_FORMAT_MAPPER] = JacksonJsonFormatMapper(objectMapper)
    hibernateProperties[AvailableSettings.INTERCEPTOR] = entityInterceptor
  }
}
