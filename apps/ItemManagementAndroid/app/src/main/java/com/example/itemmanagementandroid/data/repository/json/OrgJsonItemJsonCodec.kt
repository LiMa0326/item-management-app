package com.example.itemmanagementandroid.data.repository.json

import org.json.JSONArray
import org.json.JSONObject

class OrgJsonItemJsonCodec : ItemJsonCodec {
    override fun encodeTags(tags: List<String>): String {
        return JSONArray(tags).toString()
    }

    override fun decodeTags(tagsJson: String): List<String> {
        val array = JSONArray(tagsJson)
        return buildList {
            for (index in 0 until array.length()) {
                val value = array.get(index)
                if (value is String) {
                    val normalized = value.trim()
                    if (normalized.isNotEmpty()) {
                        add(normalized)
                    }
                }
            }
        }
    }

    override fun encodeCustomAttributes(customAttributes: Map<String, Any>): String {
        val jsonObject = JSONObject()
        customAttributes.forEach { (key, value) ->
            jsonObject.put(key, value)
        }
        return jsonObject.toString()
    }

    override fun decodeCustomAttributes(customAttributesJson: String): Map<String, Any> {
        val jsonObject = JSONObject(customAttributesJson)
        val result = linkedMapOf<String, Any>()
        val keys = jsonObject.keys()

        while (keys.hasNext()) {
            val key = keys.next()
            val value = jsonObject.get(key)
            result[key] = when (value) {
                is String -> value
                is Boolean -> value
                is Number -> value
                else -> throw IllegalArgumentException(
                    "customAttributes[$key] must be string|number|boolean."
                )
            }
        }
        return result
    }
}
