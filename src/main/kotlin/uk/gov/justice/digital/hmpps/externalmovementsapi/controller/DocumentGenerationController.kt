package uk.gov.justice.digital.hmpps.externalmovementsapi.controller

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.externalmovementsapi.access.Roles
import uk.gov.justice.digital.hmpps.externalmovementsapi.service.document.DocumentGenerationService

@RestController
@RequestMapping("/temporary-absence-authorisations")
class DocumentGenerationController(private val documentGenerationService: DocumentGenerationService) {

  @GetMapping("/supported-templates")
  fun getSupportedTemplates(): Set<String> = documentGenerationService.getSupportedTemplates()

  @GetMapping("/{id}/documents")
  fun generateDocument(@PathVariable id: String, @RequestParam format: String): ResponseEntity<ByteArray> {
    val generatedDocument = documentGenerationService.generateDocumentFromTemplate(id, format)

    val contentType = if (format.equals("pdf")) MediaType.APPLICATION_PDF else MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    val fileExtension = if (format.equals("pdf")) "pdf" else "docx"

    val headers = HttpHeaders()
    headers.contentType = contentType
    headers.setContentDispositionFormData("attachment", "tap-$id.$fileExtension")

    return ResponseEntity.ok()
      .headers(headers)
      .body(generatedDocument)
  }
}
