package com.anagorny.radiot2telegram.model

data class HashTagsWithStringValue(
        val hashtags: Set<String> = emptySet(),
        val hashtagsStr: String = ""
)

data class HashTagContainer(
        val result: Boolean = false,
        val code: Int,
        val message: String?,
        val data: List<HashTagItem> = emptyList()
) {
    fun take(takeCount: Int = 5): Set<String> {
        return data.asSequence()
                .sortedByDescending { it.exposure }
                .take(takeCount)
                .map { it.hashtag }
                .filter { !it.isNullOrEmpty() }
                .toSet()
    }
}

data class HashTagItem(
        val hashtag: String,
        val tag: String,
        val tweets: Long,
        val exposure: Long,
        val retweets: Long,
        val images: Long,
        val links: Long,
        val mentions: Long,
        val color: Int
)