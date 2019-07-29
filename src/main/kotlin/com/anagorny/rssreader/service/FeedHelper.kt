package com.anagorny.rssreader.service

fun rawDescription(descriptionBody: String, sourceUrl: String = ""): String {
    val liTags = mutableListOf<String>()
    descriptionBody.replace("<li>.+</li>".toRegex()) { liTags.add(it.value); "" }

    val aTags = mutableListOf<String>()
    liTags.forEach {liTag -> liTag.replace("<a href=.+</a>".toRegex()) { aTag -> aTags.add("&#10148; ${aTag.value}"); "" }}

    var result = aTags.joinToString("\n")

    if (sourceUrl.isNotEmpty()) result += "\n\n Подробнее - $sourceUrl"
    return result
}
