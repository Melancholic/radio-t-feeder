package com.anagorny.rssreader.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.boot.ExitCodeGenerator
import org.springframework.boot.SpringApplication





@Service
class ConsoleAppService : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(CommandLineRunner::class.java)

    @Autowired
    lateinit var applicationContext: ApplicationContext

    @Autowired
    lateinit var feeder: Feeder

    override fun run(vararg args: String?) {
        logger.info("Console app started with args: ${args.joinToString(", ")}")

        try {
            feeder.archiveProcessing()
        } catch (e: Exception) {
            logger.error("Error while archive processing", e)
            closeApp(COMMON_ERR_EXIT_CODE)
        }
        closeApp()
    }


    private fun closeApp(exitCode: Int = SUCCESS_EXIT_CODE) {
        if (exitCode != SUCCESS_EXIT_CODE) {
            SpringApplication.exit(applicationContext, ExitCodeGenerator {
                logger.info("Console app has been shutdown with exit code = $exitCode")
                return@ExitCodeGenerator exitCode
            })
        } else {
            (applicationContext as ConfigurableApplicationContext).close()
            logger.info("Console app has been closed with exit code = $exitCode")
        }
    }

    companion object {
        const val SUCCESS_EXIT_CODE = 0
        const val COMMON_ERR_EXIT_CODE = 1
    }

}