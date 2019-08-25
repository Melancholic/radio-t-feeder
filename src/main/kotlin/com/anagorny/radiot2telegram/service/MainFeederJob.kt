package com.anagorny.radiot2telegram.service

import org.quartz.CronTrigger
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component


@Component
@DisallowConcurrentExecution
class MainFeederJob : Job {
    private val logger = LoggerFactory.getLogger(MainFeederJob::class.java)
    @Autowired
    private lateinit var mainFeederService: MainFeederService

    override fun execute(context: JobExecutionContext) {
        logger.info("MainFeederJob started by trigger ${context.trigger.key.name} (${(context.trigger as CronTrigger).cronExpression})")
        mainFeederService.readRssFeed()
        logger.info("MainFeederJob finished, next call at over ${context.nextFireTime}")
    }

}
