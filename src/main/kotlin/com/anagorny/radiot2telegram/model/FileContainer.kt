package com.anagorny.radiot2telegram.model

import java.io.File

data class FileContainer (
    val file: File,
    val name: String,
    val extension: String
) {
    val fullName = "$name.$extension"
}
