package uk.gov.justice.digital.hmpps.externalmovementsapi.service.document

import org.docx4j.Docx4J
import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream

@Service
class DocumentGenerationService(private val templateHandlerRegistry: TemplateHandlerRegistry) {

  fun generateDocumentFromTemplate(templateName: String, format: String): ByteArray {
    try {
      val handler = templateHandlerRegistry.getHandler(templateName)
      val templateStream = ClassPathResource("$templateName.docx").inputStream

      val wordPackage = WordprocessingMLPackage.load(templateStream)

      val documentData = handler.buildDocumentData()

      handler.apply(wordPackage, documentData)

      val outputStream = ByteArrayOutputStream()

      if (format.equals("word")) {
        wordPackage.save(outputStream)
      } else if (format.equals("pdf")) {
        Docx4J.toPDF(wordPackage, outputStream)
      }

      return outputStream.toByteArray()
    } catch (e: Exception) {
      e.printStackTrace()
      return ByteArray(0)
    }
  }
}
