package com.example.itemmanagementandroid.ui.screens.itemedit

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object ItemEditFormMapper {
    private val DATE_WITH_SEPARATOR_REGEX = Regex("""^\d{4}[-/.]\d{1,2}[-/.]\d{1,2}$""")
    private val DATE_COMPACT_REGEX = Regex("""^\d{8}$""")
    private val DATE_OUTPUT_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE

    fun parseTags(tagsInput: String): List<String> {
        return tagsInput
            .split(',')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
    }

    fun normalizePurchaseDate(purchaseDateInput: String): String? {
        val normalized = purchaseDateInput.trim()
        if (normalized.isEmpty()) {
            return null
        }

        val canonical = when {
            DATE_WITH_SEPARATOR_REGEX.matches(normalized) -> {
                normalized.replace('/', '-').replace('.', '-')
            }

            DATE_COMPACT_REGEX.matches(normalized) -> {
                "${normalized.substring(0, 4)}-${normalized.substring(4, 6)}-${normalized.substring(6, 8)}"
            }

            else -> {
                throw IllegalArgumentException(
                    "Purchase date must be a valid date in YYYY-MM-DD format."
                )
            }
        }

        val parsedDate = runCatching {
            val parts = canonical.split('-')
            val year = parts[0].toInt()
            val month = parts[1].toInt()
            val day = parts[2].toInt()
            LocalDate.of(year, month, day)
        }.getOrElse {
            throw IllegalArgumentException("Purchase date must be a valid date in YYYY-MM-DD format.")
        }

        return parsedDate.format(DATE_OUTPUT_FORMATTER)
    }

    fun formatPurchasePrice(value: Double): String {
        return value
            .toBigDecimal()
            .setScale(2, RoundingMode.HALF_UP)
            .toPlainString()
    }

    fun parsePurchasePrice(purchasePriceInput: String): Double? {
        val normalized = purchasePriceInput.trim()
        if (normalized.isEmpty()) {
            return null
        }

        val parsed = normalized.toBigDecimalOrNull()
            ?: throw IllegalArgumentException(
                "Purchase price must be a valid number."
            )
        val normalizedPrice = parsed.setScale(2, RoundingMode.HALF_UP)
        require(normalizedPrice >= BigDecimal.ZERO) {
            "Purchase price must be greater than or equal to 0."
        }

        return normalizedPrice.toDouble()
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
