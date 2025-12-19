package uk.gov.justice.digital.hmpps.externalmovementsapi.service.document

import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import org.springframework.stereotype.Component

interface DocumentTemplateHandler {
  val templateName: String

  fun buildDocumentData(): DocumentData

  fun apply(wordprocessingMLPackage: WordprocessingMLPackage, documentData: DocumentData) {
    DocxTemplateEngine.replaceTextPlaceholders(wordprocessingMLPackage, documentData.textVariables)
    DocxTemplateEngine.replaceImagePlaceholders(wordprocessingMLPackage, documentData.imageData)
  }
}

@Component
class RotlLicenceTemplateHandler : DocumentTemplateHandler {
  override val templateName = "ROTL_LICENCE"

  override fun buildDocumentData(): DocumentData {
    // TODO: Fetch data from database

    // Build map of text variables. Key is the placeholder present in the word document, value is the value from the db.
    // E.G "PRISONER_NAME" -> "John Smith"
    // Also certain characters need to be escaped in the document, e.g. "&" -> "&amp; using the escapeXml function"
    val textVariables = mapOf<String, String>()

    // Build map of image data. Key is the placeholder present in the word document, value is the image data bytes.
    val imageData = mapOf<String, ByteArray>()

    return DocumentData(textVariables, imageData)
  }
}

data class DocumentData(val textVariables: Map<String, String>, val imageData: Map<String, ByteArray>)

private fun escapeXml(text: String): String = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
