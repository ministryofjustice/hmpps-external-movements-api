package uk.gov.justice.digital.hmpps.externalmovementsapi.service.document

import jakarta.xml.bind.JAXBElement
import org.docx4j.Docx4J
import org.docx4j.TraversalUtil
import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPartAbstractImage
import org.docx4j.wml.Drawing
import org.docx4j.wml.R
import org.docx4j.wml.Text
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.math.min

@Service
class DocumentGenerationService(private val templateHandlerRegistry: TemplateHandlerRegistry) {

  fun uploadDocumentTemplate(templateName: String, document: ByteArray): String {
    return ""
  }

  fun generateDocumentFromTemplate(templateName: String, format: String): ByteArray {
    try {
      val handler = templateHandlerRegistry.getHandler(templateName)
      val templateStream = ClassPathResource("templates/$templateName.docx").inputStream

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