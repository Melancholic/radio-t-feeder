package com.anagorny.radiot2telegram.services.impl

import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import mu.KLogging
import org.springframework.stereotype.Service
import java.net.URI

@Service
class FeedFetcher(val syndFeedInput: SyndFeedInput) {
    fun downloadFeed(feedUrl: String): SyndFeed {
        try {
            logger.info("Fetching $feedUrl...")
            return syndFeedInput.build(XmlReader(URI(feedUrl).toURL()))
        } catch (ex: Exception) {
            logger.error("Fetching RSS feed failed", ex)
            throw ex
        }
    }

    companion object : KLogging()
}