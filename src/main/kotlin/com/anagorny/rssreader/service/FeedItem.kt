package com.anagorny.rssreader.service

import java.io.File
import java.util.*

data class FeedItemWithFile (val item: FeedItem, var file: File? = null)

data class FeedItem (
        val title: String,
        val authors: String,
        val audioUrl: String?,
        val audioUrlAlter: String?,
        val audioType: String,
        val description: String,
        val descriptionType: String,
        val podcastUrl: String,
        val publishedDate: Date,
        val thumbUrl: String?
)