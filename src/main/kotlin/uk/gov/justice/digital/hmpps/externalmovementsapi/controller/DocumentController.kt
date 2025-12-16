package uk.gov.justice.digital.hmpps.externalmovementsapi.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.document.DocumentGenerationService

@RestController
@RequestMapping("/temporary-absence-authorisations")
class DocumentController(private val documentGenerationService: DocumentGenerationService) {

  @GetMapping("/{id}/documents")
  fun generateDocument(@PathVariable id: String, @RequestParam format: String) {
    val generateDocumentFromTemplate = documentGenerationService.generateDocumentFromTemplate(id, format)
  }

  @PutMapping("/documents/{templateId}")
  fun uploadDocumentTemplate(@PathVariable templateId: String, @RequestBody request: ByteArray) {
    val uploadDocumentTemplate = documentGenerationService.uploadDocumentTemplate(templateId, request)
  }
}