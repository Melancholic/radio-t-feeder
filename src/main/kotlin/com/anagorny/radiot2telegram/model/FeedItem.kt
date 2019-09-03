package com.anagorny.radiot2telegram.model

import java.util.*

class FeedItemWithFile(val item: FeedItem, var filePath: String)

data class FeedItem(
        val title: String,
        val authors: String,
        val audioUrl: String?,
        val audioUrlAlter: String?,
        val audioType: String,
        val description: String,
        val descriptionType: String,
        val podcastUrl: String,
        val publishedDate: Date,
        val thumbUrl: String?,
        val hashtags: Set<String> = emptySet()
) {
    var tgMessageId: Int? = null
    var tgFileId: String? = null
}