package uk.gov.justice.digital.hmpps.externalmovementsapi.service.document

import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import org.springframework.stereotype.Component

interface DocumentTemplateHandler {
  val templateName: String

  fun buildDocumentData(): DocumentData

  fun apply(wordprocessingMLPackage: WordprocessingMLPackage, documentData: DocumentData) {
    DocxTemplateEngine.replaceTextPlaceholders(wordprocessingMLPackage, documentData.textVariables)
  }
}

@Component
class RotlDocumentTemplateHandler : DocumentTemplateHandler {
  override val templateName = "ROTL"

  override fun buildDocumentData(): DocumentData {
    // Fetch data from database

    // Build map of text variables. Key is the placeholder present in the word document, value is the value from the db.
    // E.G "PRISONER_NAME" -> "John Smith"
    val textVariables = mapOf<String, String>()

    return DocumentData(textVariables)
  }
}

data class DocumentData(val textVariables: Map<String, String>)