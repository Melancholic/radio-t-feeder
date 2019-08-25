package com.anagorny.radiot2telegram.service

import com.anagorny.radiot2telegram.model.MetaInfoContainer
import org.quartz.JobKey
import org.quartz.Scheduler
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.ExitCodeGenerator
import org.springframework.boot.SpringApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Service


@Service
class ConsoleAppService : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(CommandLineRunner::class.java)


    @Autowired
    lateinit var applicationContext: ApplicationContext

    @Autowired
    lateinit var archiveFeederService: ArchiveFeederService

    @Autowired
    lateinit var metaInfoContainer: MetaInfoContainer

    @Autowired
    lateinit var mainFeedFetcherScheduler: Scheduler

    @Autowired
    lateinit var mainRssFeedJobKey: JobKey

    override fun run(vararg args: String?) {
        logger.info("Console app started with args: ${args.joinToString(", ")}")

        while (!archiveFeederService.archiveIsSynced()) {
            try {
                logger.info("Archive feed need synced...")
                archiveFeederService.archiveProcessing()
            } catch (e: Exception) {
                logger.error("Error while archive processing", e)
//                    closeApp(COMMON_ERR_EXIT_CODE)
            } finally {
                metaInfoContainer.commit()
            }
        }

        logger.info("Archive feed has been synced, start scheduler for MainFeedFetcher...")


        try {
            mainFeedFetcherScheduler.start()
            val nextFireTiem = mainFeedFetcherScheduler.getTriggersOfJob(mainRssFeedJobKey).asSequence()
                    .map { it.key.name to it.nextFireTime }
                    .minBy { it.second }
            logger.info("Scheduler for MainFeedFetcher has been started, next fire time = '${nextFireTiem?.second}' by trigger ='${nextFireTiem?.first}'")
        } catch (e: Exception) {
            logger.error("Error while starting MainFeedFetcher scheduler", e)
        }

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