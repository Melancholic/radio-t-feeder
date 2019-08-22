package com.anagorny.radiot2telegram.service

import com.anagorny.radiot2telegram.model.FeedItem
import com.rometools.rome.feed.synd.SyndEntry

fun parseDescription(descriptionBody: String, sourceUrl: String = ""): String {
    val liTags = mutableListOf<String>()
    descriptionBody.replace("<li>.+</li>".toRegex()) { liTags.add(("&#10148; ${it.value}")); "" }

    var result = liTags.asSequence()
            .map {
                return@map it.replace("<li>", "")
                        .replace("</li>", ";")
            }
            .joinToString("\n")

    if (sourceUrl.isNotEmpty()) result += "\n\n Подробнее - $sourceUrl"
    return result
}

fun parseAudioUrl(descriptionBody: String): String? {
    var audioTag: String = ""

    descriptionBody.replace("<audio.+>".toRegex()) { audioTag = it.value; "" }


    var sourceUrl: String? = null
    audioTag.replace("src=\"[^\"]+\"".toRegex()) { sourceUrl = it.value.replace("src=\"", "").replace("\"", ""); "" }

    return sourceUrl
}


fun buildFeedItem(entry: SyndEntry): FeedItem {
    val description = parseDescription(entry.description?.value ?: "", entry.uri)
    val audioUrlAlter = entry.enclosures.firstOrNull()?.url
    val audioUrl = parseAudioUrl(entry.description?.value ?: "") ?: audioUrlAlter
    return FeedItem(
            title = entry.title,
            authors = entry.authors.joinToString { ", " },
            audioType = entry.enclosures.firstOrNull()?.type ?: "",
            audioUrlAlter = audioUrlAlter,
            audioUrl = audioUrl,
            description = description,
            descriptionType = entry.description?.type ?: "",
            podcastUrl = entry.uri,
            publishedDate = entry.publishedDate,
            thumbUrl = entry.foreignMarkup?.first { x -> x.name == "image" }?.attributes?.first()?.value
    )
}