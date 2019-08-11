package com.anagorny.rssreader.model

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.io.FileInputStream
import java.util.*
import kotlin.collections.LinkedHashMap


data class MetaInfoEntity(
        var lastUpdateTime: Date? = Date(),
        var lastOffset: Int? = null,
        var lastTitle: String? = null,
        var messages: MutableMap<Int, FeedItem> = LinkedHashMap()

)

class MetaInfoContainer(srcMetaFile: String, private val mapper: ObjectMapper) {
    private val file: File = File(srcMetaFile)
    val metaInfoEntity: MetaInfoEntity

    init {
        metaInfoEntity = if (file.exists()) {
            FileInputStream(file).use { fileStream -> mapper.readValue(fileStream, MetaInfoEntity::class.java) }
        } else {
            MetaInfoEntity()
        }
    }

    fun append(offset: Int, feed: FeedItem, autoCommit: Boolean = true) {
        metaInfoEntity.apply {
            lastUpdateTime = Date()
            lastTitle = feed.title
            lastOffset = offset
        }
        metaInfoEntity.messages[offset] = feed
        if (autoCommit) commit()
    }

    fun commit() {
        val writer = mapper.writer(DefaultPrettyPrinter())
        writer.writeValue(file, metaInfoEntity)
    }
}