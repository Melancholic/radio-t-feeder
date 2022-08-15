package com.anagorny.radiot2telegram.services

import com.anagorny.radiot2telegram.model.FileContainer
import org.springframework.stereotype.Component

@Component
interface IConverter {
    fun convert(src: FileContainer) : FileContainer
}