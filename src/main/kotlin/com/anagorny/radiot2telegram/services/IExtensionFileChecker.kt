package com.anagorny.radiot2telegram.services

import org.springframework.stereotype.Component

@Component
interface IExtensionFileChecker {
    fun check(fileName: String) : Boolean
}
