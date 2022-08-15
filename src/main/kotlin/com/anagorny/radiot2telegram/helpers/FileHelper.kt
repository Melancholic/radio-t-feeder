package com.anagorny.radiot2telegram.helpers

import org.slf4j.Logger
import java.io.File


fun removeFile(file: File, logger: Logger) {
    try {
        val path = file.absolutePath
        if (file.delete()) {
            logger.info("File $path deleted")
        } else {
            throw Exception("File $path cant be deleted")
        }
    } catch (e: Exception) {
        logger.error("Error while removing file", e)
    }
}

fun removeFile(filePath: String, logger: Logger) {
    try {
        return removeFile(File(filePath), logger)
    } catch (e: Exception) {
        logger.error("Error while remove file '$filePath'", e)
    }
}

fun removeFileIfExist(filePath: String?, logger: Logger) {
    if (!filePath.isNullOrBlank()) {
        val file = File(filePath)
        if (file.exists()) {
            if (file.delete()) {
                logger.info("File $filePath deleted successfully")
            } else {
                throw Exception("file $filePath cant be deleted")
            }
        }
    }
}