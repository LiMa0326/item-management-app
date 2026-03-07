package com.example.itemmanagementandroid.ui.screens.itemedit

object ItemEditFormMapper {
    fun parseTags(tagsInput: String): List<String> {
        return tagsInput
            .split(',')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
    }

    fun parsePurchasePrice(purchasePriceInput: String): Double? {
        val normalized = purchasePriceInput.trim()
        if (normalized.isEmpty()) {
            return null
        }

        val parsed = normalized.toDoubleOrNull()
            ?: throw IllegalArgumentException("Purchase price must be a valid number.")

        require(parsed.isFinite()) {
            "Purchase price must be a valid number."
        }

        return parsed
    }

    fun parseCustomAttributes(
        rows: List<ItemEditCustomAttributeRowUiModel>
    ): Map<String, Any> {
        val attributes = linkedMapOf<String, Any>()

        rows.forEach { row ->
            val key = row.key.trim()
            val rawValue = row.value.trim()

            if (key.isEmpty() && rawValue.isEmpty()) {
                return@forEach
            }

            require(key.isNotEmpty()) {
                "Custom attribute key cannot be empty."
            }
            require(!attributes.containsKey(key)) {
                "Custom attribute key \"$key\" is duplicated."
            }

            attributes[key] = parseCustomAttributeValue(rawValue)
        }

        return attributes
    }

    private fun parseCustomAttributeValue(rawValue: String): Any {
        if (rawValue.equals("true", ignoreCase = true)) {
            return true
        }
        if (rawValue.equals("false", ignoreCase = true)) {
            return false
        }

        rawValue.toLongOrNull()?.let { parsed ->
            return parsed
        }

        rawValue.toDoubleOrNull()
            ?.takeIf(Double::isFinite)
            ?.let { parsed ->
                return parsed
            }

        return rawValue
    }
}
