package com.anagorny.radiot2telegram.services.impl

import com.anagorny.radiot2telegram.config.HashTagsSuggestionProperties
import com.anagorny.radiot2telegram.helpers.rawDescription
import com.anagorny.radiot2telegram.model.HashTagContainer
import com.anagorny.radiot2telegram.model.HashTagsWithStringValue
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder


@Service
class HashTagsSuggestionService(
    private val hashTagsSuggestionProperties: HashTagsSuggestionProperties
) {
    val logger = LoggerFactory.getLogger(HashTagsSuggestionService::class.java)


    fun getHashtagsFromDescription(title: String, description: String): HashTagsWithStringValue {

        if (!hashTagsSuggestionProperties.enabled) {
            logger.warn("HashtagsSuggestion is disabled, skipping hashtags fetching for '$title'...")
            return HashTagsWithStringValue()
        }

        logger.info("HashtagsSuggestion for '$title' running...")

        val sanitizedDescription = rawDescription(description)

        if (sanitizedDescription.isNullOrEmpty()) {
            logger.warn("HashtagsSuggestion is impossible, because source content has empty, skipping hashtags fetching for '$title'...")
            return HashTagsWithStringValue()
        }

        val uriBuilder = UriComponentsBuilder.fromHttpUrl(hashTagsSuggestionProperties.api.url)
            .queryParam("client_id", hashTagsSuggestionProperties.api.token)
            .queryParam("text", sanitizedDescription).build()

        return try {
            val hashTagContainer = RestTemplate().getForObject(uriBuilder.toUriString(), HashTagContainer::class.java)

            logger.info(
                "HashtagsSuggestion returned ${
                    hashTagContainer?.data?.count() ?: 0
                } different hashtags for '$title'"
            )
            val hashtags = hashTagContainer?.take(hashTagsSuggestionProperties.count) ?: emptySet()

            HashTagsWithStringValue(hashtags, hashtags.asSequence().map { "#$it" }.joinToString(", "))
        } catch (e: Exception) {
            logger.warn("Cant fetch hashtags suggestions for '$title', skipping...", e)
            HashTagsWithStringValue()
        }
    }

}