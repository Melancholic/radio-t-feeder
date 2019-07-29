package com.anagorny.rssreader.config

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@ComponentScan("com.anagorny.rssreader")
@EnableScheduling
class SpringContext
