package com.anagorny.radiot2telegram.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class MainController {
    @GetMapping("/")
    fun homePage(): String {
        return "OK"
    }
}