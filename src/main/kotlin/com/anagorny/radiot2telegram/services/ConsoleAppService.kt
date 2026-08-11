package com.anagorny.radiot2telegram.services

import com.anagorny.radiot2telegram.config.SystemProperties
import com.anagorny.radiot2telegram.model.MetaInfoContainer
import com.anagorny.radiot2telegram.services.impl.ArchiveFeederService
import mu.KLogging
import org.quartz.JobKey
import org.quartz.Scheduler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Service


@Service
class ConsoleAppService : CommandLineRunner {
    @Autowired
    lateinit var archiveFeederService: ArchiveFeederService

    @Autowired
    lateinit var metaInfoContainer: MetaInfoContainer

    @Autowired
    lateinit var systemProperties: SystemProperties

    @Autowired
    lateinit var mainFeedFetcherScheduler: Scheduler

    @Autowired
    lateinit var mainRssFeedJobKey: JobKey

    override fun run(vararg args: String?) {
        logger.info("Console app started with args: ${args.joinToString(", ")}")

        validateInitState()

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

    private fun validateInitState() {
        val hasProcessedData = metaInfoContainer.metaInfoEntity.lastPublishedTime != null

        if (!hasProcessedData) {
            if (!systemProperties.initMode) {
                logger.error("No processed data found and INIT_MODE is not enabled, initialization is not allowed. Set INIT_MODE=1 to perform the initial archive backfill.")
                throw IllegalStateException("No processed data found, initialization is not allowed (INIT_MODE=0 or absent)")
            }
            logger.info("No processed data found, INIT_MODE=1 - initial archive backfill will be performed.")
        } else {
            val meta = metaInfoContainer.metaInfoEntity
            logger.info("Existing metadata found (lastIndex=${meta.lastIndex}, lastPublishedTime=${meta.lastPublishedTime}), skipping initialization and proceeding to sync new entries and start the scheduler.")
        }
    }

    private companion object : KLogging()
}