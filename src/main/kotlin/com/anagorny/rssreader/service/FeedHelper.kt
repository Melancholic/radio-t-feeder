package com.anagorny.rssreader.service

fun rawDescription(descriptionBody: String, sourceUrl: String = ""): String {
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
