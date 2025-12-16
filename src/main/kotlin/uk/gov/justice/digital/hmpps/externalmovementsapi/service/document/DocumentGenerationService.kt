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
class DocumentGenerationService {

  companion object {
    private const val MAX_IMAGE_SIZE_INCHES = 1.0
    private const val EMU_PER_INCH = 914400L
  }

  fun uploadDocumentTemplate(templateName: String, document: ByteArray): String {
    return ""
  }

  fun generateDocumentFromTemplate(templateName: String, format: String): ByteArray {
    try {
      val templateStream = ClassPathResource("templates/$templateName.docx").inputStream

      val wordPackage = WordprocessingMLPackage.load(templateStream)
      val mainDocumentPart = wordPackage.mainDocumentPart

      val data = mapOf<String, String>()

      mainDocumentPart.variableReplace(data)

      updateImagePlaceholder(wordPackage, "PROFILE_IMAGE_PLACEHOLDER")

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

  private fun updateImagePlaceholder(wordprocessingMLPackage: WordprocessingMLPackage, placeHolder: String) {
    // Get image as a byte array
    val profileImageBytes = ClassPathResource("profilePic.jpg").inputStream.use { it.readBytes() }

    val maxSizeEmu = (MAX_IMAGE_SIZE_INCHES * EMU_PER_INCH).toLong()
    val emuSize = calculateImageSize(profileImageBytes, maxSizeEmu)

    val imagePart = BinaryPartAbstractImage.createImagePart(wordprocessingMLPackage, profileImageBytes)

    val inline = imagePart.createImageInline(placeHolder, placeHolder, 0, 1, emuSize.width, emuSize.height, false)

    var targetText: Text? = null

    TraversalUtil(wordprocessingMLPackage.mainDocumentPart.content, object : TraversalUtil.CallbackImpl() {
      override fun apply(o: Any?): List<Any?>? {
        val unwrapped = if (o is JAXBElement<*>) o.value else o

        if (unwrapped is Text && unwrapped.value == placeHolder) {
          targetText = unwrapped
        }

        return null
      }
    })

    targetText!!.value = ""

    val run = targetText.parent as R

    val drawing = Drawing().apply {
      anchorOrInline.add(inline)
    }

    run.content.clear()
    run.content.add(drawing)
  }

  private fun calculateImageSize(imageBytes: ByteArray, maxSizeEmu: Long): ImageScale {
    val image = ImageIO.read(ByteArrayInputStream(imageBytes))

    val emuPerPixel = EMU_PER_INCH / 96.0
    val widthEmu = image.width * emuPerPixel
    val heightEmu = image.height * emuPerPixel

    val scale = min(maxSizeEmu / widthEmu, maxSizeEmu / heightEmu)

    return ImageScale((widthEmu * scale).toLong(), (heightEmu * scale).toLong())
  }

  private data class ImageScale(val width: Long, val height: Long)
}