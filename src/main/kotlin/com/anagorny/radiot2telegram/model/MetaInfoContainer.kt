package com.anagorny.radiot2telegram.model

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.util.*


data class MetaInfoEntity(
        var updateTime: Date? = Date(),
        var lastIndex: Int = -1,
        var lastTitle: String? = null,
        var lastPublishedTime: Date? = null,
        var messages: MutableMap<Int, FeedItem> = LinkedHashMap()

)

class MetaInfoContainer(srcMetaFile: String, private val mapper: ObjectMapper) {
    private val file: File = File(srcMetaFile)
    val metaInfoEntity: MetaInfoEntity = if (file.exists()) {
        try {
            FileInputStream(file).use { fileStream -> mapper.readValue(fileStream, MetaInfoEntity::class.java) }
        } catch (e: Exception) {
            logger.error("Cant read metadata", e)
            MetaInfoEntity()
        }
    } else {
        MetaInfoEntity()
    }

    fun appendToEnd(feed: FeedItem, autoCommit: Boolean = true): Int {
        metaInfoEntity.apply {
            updateTime = Date()
            lastTitle = feed.title
            lastIndex += 1
            lastPublishedTime = feed.publishedDate
        }
        metaInfoEntity.messages[metaInfoEntity.lastIndex] = feed
        if (autoCommit) commit()
        return metaInfoEntity.lastIndex
    }


    fun commit() {
        val writer = mapper.writer(DefaultPrettyPrinter())
        writer.writeValue(file, metaInfoEntity)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(MetaInfoContainer::class.java)
    }

}