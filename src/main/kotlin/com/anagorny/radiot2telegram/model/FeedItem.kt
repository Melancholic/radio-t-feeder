package com.anagorny.radiot2telegram.model

import java.util.*

data class AudioMetaInfo(
        val title: String? = null,
        val artist: String? = null,
        val album: String? = null,
        val genre: String? = null,
        val duration: Int = 0
)

data class FeedItemWithFile(val item: FeedItem, var filePath: String, var metaInfo: AudioMetaInfo = AudioMetaInfo())

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