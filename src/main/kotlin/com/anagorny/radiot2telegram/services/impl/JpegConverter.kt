package com.anagorny.radiot2telegram.services.impl

import com.anagorny.radiot2telegram.helpers.removeFile
import com.anagorny.radiot2telegram.model.FileContainer
import com.anagorny.radiot2telegram.services.IConverter
import org.apache.commons.io.FilenameUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO


@Service
class JpegConverter : IConverter {
    private val logger = LoggerFactory.getLogger(JpegConverter::class.java)

    override fun convert(src: FileContainer): FileContainer {
        val srcPath = src.file.absolutePath

        val outPath = FilenameUtils.getFullPath(srcPath)
        val outName = src.name
        val outFilePath = "$outPath$outName.$extension"
        val outImg = File(outFilePath)

        val srcImg: BufferedImage = ImageIO.read(src.file)

        val newBufferedImage = BufferedImage(
            srcImg.width, srcImg.height, BufferedImage.TYPE_INT_RGB
        )

        newBufferedImage.createGraphics()
            .drawImage(srcImg, 0, 0, Color.WHITE, null)

        if (!ImageIO.write(newBufferedImage, "jpg", outImg)) {
            val msg = "Couldn't convert from $srcPath to $outFilePath"
            logger.error(msg)
            throw java.lang.IllegalStateException(msg)
        }

        logger.info("File converted from $srcPath to ${outImg.absolutePath}")
        removeFile(src.file, logger)
        return FileContainer(outImg, outName, extension)
    }

    companion object {
        const val extension = "jpg"
    }
}