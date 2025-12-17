package uk.gov.justice.digital.hmpps.externalmovementsapi.service.document

import jakarta.xml.bind.JAXBElement
import org.docx4j.TraversalUtil
import org.docx4j.openpackaging.packages.WordprocessingMLPackage
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPartAbstractImage
import org.docx4j.wml.Drawing
import org.docx4j.wml.R
import org.docx4j.wml.Text
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.math.min

object DocxTemplateEngine {

  private const val MAX_IMAGE_SIZE_INCHES = 1.0
  private const val EMU_PER_INCH = 914400L

  fun replaceTextPlaceholders(wordprocessingMLPackage: WordprocessingMLPackage, data: Map<String, String>) {
    if (data.isEmpty()) return

    wordprocessingMLPackage.mainDocumentPart.variableReplace(data)
  }

  fun replaceImagePlaceholder(wordprocessingMLPackage: WordprocessingMLPackage, placeHolder: String, imageBytes: ByteArray) {
    val emuSize = calculateImageSize(imageBytes)

    val imagePart = BinaryPartAbstractImage.createImagePart(wordprocessingMLPackage, imageBytes)

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

  private fun calculateImageSize(imageBytes: ByteArray, maxSizeEmu: Long = (MAX_IMAGE_SIZE_INCHES * EMU_PER_INCH).toLong()): ImageScale {
    val image = ImageIO.read(ByteArrayInputStream(imageBytes))

    val emuPerPixel = EMU_PER_INCH / 96.0
    val widthEmu = image.width * emuPerPixel
    val heightEmu = image.height * emuPerPixel

    val scale = min(maxSizeEmu / widthEmu, maxSizeEmu / heightEmu)

    return ImageScale((widthEmu * scale).toLong(), (heightEmu * scale).toLong())
  }

  private data class ImageScale(val width: Long, val height: Long)
}