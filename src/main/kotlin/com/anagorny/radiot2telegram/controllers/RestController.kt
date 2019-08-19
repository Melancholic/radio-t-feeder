package com.anagorny.radiot2telegram.controllers

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController


@RestController
open class RestController {
    @GetMapping("/check")
    fun check(): String {
        return "Application is alive"
    }
}