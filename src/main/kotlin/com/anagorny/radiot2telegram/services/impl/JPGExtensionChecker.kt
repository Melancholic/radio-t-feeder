package com.anagorny.radiot2telegram.services.impl

import com.anagorny.radiot2telegram.services.IExtensionFileChecker
import org.apache.commons.io.FilenameUtils
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
@Qualifier("jpgExtensionChecker")
class JPGExtensionChecker : IExtensionFileChecker {

    override fun check(fileName: String): Boolean {
        return FilenameUtils.isExtension(fileName, extensions)
    }

    companion object {
        val extensions = listOf("jpg", "jpeg", "JPG", "JPEG")
    }
}