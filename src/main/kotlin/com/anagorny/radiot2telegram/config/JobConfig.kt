package com.anagorny.radiot2telegram.config

import com.anagorny.radiot2telegram.service.MainFeederJob
import org.quartz.JobBuilder
import org.quartz.JobDetail
import org.quartz.Scheduler
import org.quartz.SchedulerException
import org.quartz.SimpleScheduleBuilder.simpleSchedule
import org.quartz.TriggerBuilder.newTrigger
import org.quartz.impl.StdSchedulerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.quartz.SpringBeanJobFactory

@Configuration
class JobConfig {
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
    @Throws(SchedulerException::class)
    fun backgroundDecodeJob(mainFeedFetcherScheduler: Scheduler): JobDetail {
        // define the job and tie it to our MyJob class
        val job = JobBuilder.newJob(MainFeederJob::class.java)
                .withIdentity("mainFeederJob", "rssReadGroup")
                .build()

        //TODO Cron
        val trigger = newTrigger()
                .withIdentity("trigger1", "group1")
                .startNow()
                .withSchedule(simpleSchedule()
                        .withIntervalInSeconds(5)
                        .repeatForever())
                .build()

        // Tell quartz to schedule the job using our trigger
        mainFeedFetcherScheduler.scheduleJob(job, trigger)
        return job
    }

}