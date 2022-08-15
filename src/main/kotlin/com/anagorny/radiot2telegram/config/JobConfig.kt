package com.anagorny.radiot2telegram.config

import com.anagorny.radiot2telegram.services.impl.MainFeederJob
import org.quartz.*
import org.quartz.impl.StdSchedulerFactory
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.quartz.SpringBeanJobFactory


@Configuration
class JobConfig(
    private val rssProperties: RssProperties
) {

    private val logger = LoggerFactory.getLogger(JobConfig::class.java)

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Bean
    fun springBeanJobFactory(): SpringBeanJobFactory {
        val jobFactory = QuartzJobFactory()
        jobFactory.setApplicationContext(applicationContext)
        return jobFactory
    }


    @Bean
    @Throws(SchedulerException::class)
    fun mainFeedFetcherScheduler(springBeanJobFactory: SpringBeanJobFactory): Scheduler {
        val scheduler = StdSchedulerFactory.getDefaultScheduler()
        scheduler.setJobFactory(springBeanJobFactory)
        return scheduler
    }

    @Bean
    fun mainRssFeedJobKey() = JobKey("mainFeederJob", "mainRssReadGroup")

    @Bean
    @Throws(SchedulerException::class)
    fun backgroundDecodeJob(mainFeedFetcherScheduler: Scheduler, mainRssFeedJobKey: JobKey): JobDetail {

        val job = JobBuilder.newJob(MainFeederJob::class.java)
            .withIdentity(mainRssFeedJobKey)
            .build()

        val triggers = rssProperties.main.cron.asSequence()
            .filter { CronExpression.isValidExpression(it) }
            .mapIndexed { index, expression ->
                try {
                    return@mapIndexed TriggerBuilder
                        .newTrigger()
                        .withIdentity("cronTrigger#$index", "mainRssReaderJob")
                        .withSchedule(CronScheduleBuilder.cronSchedule(expression))
                        .build()
                } catch (e: Exception) {
                    logger.error("Building cron trigger by expression='$expression' is failed, skipping...", e)
                    return@mapIndexed null
                }

            }
            .filter { it != null }
            .toSet()

        if (triggers.isEmpty()) {
            throw RuntimeException("Initializing job '${mainRssFeedJobKey.name}' has been failed, because there are not active triggers!")
        }
        mainFeedFetcherScheduler.scheduleJob(job, triggers, false)

        logger.info("Job '${mainRssFeedJobKey.name}' has been initialized with ${
            mainFeedFetcherScheduler.getTriggersOfJob(mainRssFeedJobKey)
                .joinToString { "${it.key.name} (${(it as CronTrigger).cronExpression})" }
        }"
        )
        return job
    }

}