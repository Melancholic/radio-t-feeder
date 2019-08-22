package com.anagorny.radiot2telegram.service

import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FeedFetcher {
    private val logger = LoggerFactory.getLogger(FeedFetcher::class.java)

    fun downloadFeed(feedUrl: String): SyndFeed {
        try {
            HttpClients.createMinimal().use { client ->
                logger.info("downloading $feedUrl")
                val request = HttpGet(feedUrl)
                client.execute(request).use { response ->
                    response.entity.content.use { stream ->
                        if (stream.available() > 0) {
                            val input = SyndFeedInput()
                            return input.build(XmlReader(stream, true /* lenient */))
                        } else {
                            throw RuntimeException("Skipping feed due to zero input")
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            logger.error("HttpClient failure", ex)
            throw ex
        }
    }

}