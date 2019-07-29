package com.anagorny.rssreader.service

import org.springframework.http.HttpStatus
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController


@RestController
open class MainController {

    @RequestMapping("/")
    @ResponseStatus(HttpStatus.OK)
    fun home(model: Model): String {
        return "OK"
    }

}