package com.example.itemmanagementandroid.backup.export

object BackupJsonEncoder {
    fun encode(value: Any?): String {
        return buildString {
            appendJsonValue(this, value)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun appendJsonValue(
        builder: StringBuilder,
        value: Any?
    ) {
        when (value) {
            null -> builder.append("null")
            is String -> appendEscapedString(builder, value)
            is Boolean,
            is Int,
            is Long,
            is Float,
            is Double,
            is Byte,
            is Short -> builder.append(value.toString())
            is Map<*, *> -> appendJsonObject(builder, value as Map<String, Any?>)
            is List<*> -> appendJsonArray(builder, value)
            else -> appendEscapedString(builder, value.toString())
        }
    }

    private fun appendJsonObject(
        builder: StringBuilder,
        value: Map<String, Any?>
    ) {
        builder.append('{')
        value.entries.forEachIndexed { index, entry ->
            if (index > 0) {
                builder.append(',')
            }
            appendEscapedString(builder, entry.key)
            builder.append(':')
            appendJsonValue(builder, entry.value)
        }
        builder.append('}')
    }

    private fun appendJsonArray(
        builder: StringBuilder,
        value: List<*>
    ) {
        builder.append('[')
        value.forEachIndexed { index, item ->
            if (index > 0) {
                builder.append(',')
            }
            appendJsonValue(builder, item)
        }
        builder.append(']')
    }

    private fun appendEscapedString(
        builder: StringBuilder,
        value: String
    ) {
        builder.append('"')
        value.forEach { character ->
            when (character) {
                '\\' -> builder.append("\\\\")
                '"' -> builder.append("\\\"")
                '\b' -> builder.append("\\b")
                '\u000C' -> builder.append("\\f")
                '\n' -> builder.append("\\n")
                '\r' -> builder.append("\\r")
                '\t' -> builder.append("\\t")
                else -> {
                    if (character.code in 0..0x1F) {
                        builder.append("\\u")
                        builder.append(character.code.toString(16).padStart(4, '0'))
                    } else {
                        builder.append(character)
                    }
                }
            }
        }
        builder.append('"')
    }
}

