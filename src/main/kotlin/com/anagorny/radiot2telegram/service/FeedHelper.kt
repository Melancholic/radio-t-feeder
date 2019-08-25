package com.anagorny.radiot2telegram.service

fun parseDescription(descriptionBody: String, sourceUrl: String = ""): String {
    val liTags = mutableListOf<String>()
    descriptionBody.replace("<li>.+</li>".toRegex()) { liTags.add(("&#10148; ${it.value}")); "" }

    var result = liTags.asSequence()
            .map {
                return@map it.replace("<li>", "")
                        .replace("</li>", "")
            }
            .joinToString("\n")

    if (sourceUrl.isNotEmpty()) result += "\n\n Подробнее - $sourceUrl"
    return result
}

fun parseAudioUrl(descriptionBody: String): String? {
    var audioTag = ""

    descriptionBody.replace("<audio.+>".toRegex()) { audioTag = it.value; "" }


    var sourceUrl: String? = null
    audioTag.replace("src=\"[^\"]+\"".toRegex()) { sourceUrl = it.value.replace("src=\"", "").replace("\"", ""); "" }

    return sourceUrl
}
