package com.anagorny.radiot2telegram.service

import com.anagorny.radiot2telegram.helpers.rawDescription
import com.anagorny.radiot2telegram.model.HashTagContainer
import com.anagorny.radiot2telegram.model.HashTagsWithStringValue
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder


@Service
class HashTagsSuggestionService {
    val logger = LoggerFactory.getLogger(HashTagsSuggestionService::class.java)

    @Value("\${radio_t_feeder.hashtags.suggestion.enabled}")
    private var suggestionEnabled: Boolean = false

    @Value("\${radio_t_feeder.hashtags.suggestion.count}")
    private var hashtagsCount: Int = 5

    @Value("\${radio_t_feeder.hashtags.suggestion.api.secret_client_id}")
    private lateinit var secretClientId: String

    @Value("\${radio_t_feeder.hashtags.suggestion.api.url}")
    private lateinit var apiUrl: String

    fun getHashtagsFromDescription(title: String, description: String): HashTagsWithStringValue {

        if (!suggestionEnabled) {
            logger.warn("HashtagsSuggestion is disabled, skipping hashtags fetching for '$title'...")
            return HashTagsWithStringValue()
        }

        logger.info("HashtagsSuggestion for '$title' running...")

        val sanitizedDescription = rawDescription(description)

        if (sanitizedDescription.isNullOrEmpty()) {
            logger.warn("HashtagsSuggestion is impossible, because source content has empty, skipping hashtags fetching for '$title'...")
            return HashTagsWithStringValue()
        }

        val uriBuilder = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParam("client_id", secretClientId)
                .queryParam("text", sanitizedDescription)
                .build()

        return try {
            val hashTagContainer = RestTemplate().getForObject(uriBuilder.toUriString(), HashTagContainer::class.java)

            logger.info("HashtagsSuggestion returned ${hashTagContainer?.data?.count()
                    ?: 0} different hashtags for '$title'")
            val hashtags = hashTagContainer?.take(hashtagsCount) ?: emptySet()

            HashTagsWithStringValue(hashtags, hashtags.asSequence().map { "#$it" }.joinToString(", "))
        } catch (e: Exception) {
            logger.warn("Cant fetch hashtags suggestions for '$title', skipping...", e)
            HashTagsWithStringValue()
        }
    }

}