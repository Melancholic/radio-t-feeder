package com.anagorny.radiot2telegram.services

import com.anagorny.radiot2telegram.model.MetaInfoContainer
import com.anagorny.radiot2telegram.services.impl.ArchiveFeederService
import org.quartz.JobKey
import org.quartz.Scheduler
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.context.ApplicationContext
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
                logger.info("Archive feed need to sync...")
                archiveFeederService.archiveProcessing()
            } catch (e: Exception) {
                logger.error("Error while archive feed processing", e)
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
            logger.info("Scheduler for MainFeedFetcher has been started, next fire time = '${nextFireTiem.second}' by trigger ='${nextFireTiem.first}'")
        } catch (e: Exception) {
            logger.error("Error while starting MainFeedFetcher scheduler", e)
        }

    }

}