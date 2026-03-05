package com.example.itemmanagementandroid.data.repository.json

interface ItemJsonCodec {
    fun encodeTags(tags: List<String>): String
    fun decodeTags(tagsJson: String): List<String>
    fun encodeCustomAttributes(customAttributes: Map<String, Any>): String
    fun decodeCustomAttributes(customAttributesJson: String): Map<String, Any>
}
