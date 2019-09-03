package com.anagorny.radiot2telegram.helpers

import org.slf4j.Logger
import java.io.File


fun removeFile(file: File, logger: Logger) {
    try {
        val path = file.absolutePath
        if (file.delete()) {
            logger.info("File $path deleted successfully")
        } else {
            throw Exception("file $path cant be deleted")
        }
    } catch (e: Exception) {
        logger.error("Error while remove file", e)
    }
}

fun removeFile(filePath: String, logger: Logger) {
    try {
        return removeFile(File(filePath), logger)
    } catch (e: Exception) {
        logger.error("Error while remove file '$filePath'", e)
    }
}