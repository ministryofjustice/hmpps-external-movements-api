package uk.gov.justice.digital.hmpps.externalmovementsapi.service.document

import org.springframework.stereotype.Component

@Component
class TemplateHandlerRegistry(templateHandlers: List<DocumentTemplateHandler>) {
  private val handlerMap = templateHandlers.associateBy { it.templateName }

  fun getHandler(templateName: String): DocumentTemplateHandler = handlerMap[templateName] ?: error("No handler found for template $templateName")

  fun getSupportedTemplates() = handlerMap.keys
}
