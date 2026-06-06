package com.pausecard.app.groq

import com.pausecard.app.util.CrashReporting

object CardValidator {

    private const val TAG = "CardValidator"
    private const val MAX_TITLE_WORDS = 8
    private const val MAX_BODY_SENTENCES = 4
    private const val MAX_BODY_CHARS = 500

    val validCategories = setOf(
        "ECE", "Cybersecurity", "Programming", "Tamil", "General", "LLM", "Android",
        "Automation", "DevTools"
    )

    data class ValidationResult(
        val isValid: Boolean,
        val title: String = "",
        val body: String = "",
        val category: String = "",
        val errors: List<String> = emptyList()
    )

    fun validate(title: String?, body: String?, category: String?): ValidationResult {
        val errors = mutableListOf<String>()

        val cleanTitle = title?.trim() ?: ""
        val cleanBody = body?.trim() ?: ""
        val cleanCategory = category?.trim() ?: ""

        if (cleanTitle.isEmpty()) {
            errors.add("Title is empty")
        } else if (cleanTitle.split("\\s+".toRegex()).size > MAX_TITLE_WORDS) {
            errors.add("Title exceeds $MAX_TITLE_WORDS words")
        }

        if (cleanBody.isEmpty()) {
            errors.add("Body is empty")
        } else {
            val sentences = cleanBody.split(Regex("[.!?]+")).filter { it.trim().isNotEmpty() }
            if (sentences.size != MAX_BODY_SENTENCES) {
                errors.add("Body has ${sentences.size} sentences, expected $MAX_BODY_SENTENCES")
            }
            if (cleanBody.length > MAX_BODY_CHARS) {
                errors.add("Body exceeds $MAX_BODY_CHARS characters")
            }
        }

        if (cleanCategory.isEmpty()) {
            errors.add("Category is empty")
        } else if (cleanCategory !in validCategories) {
            errors.add("Invalid category: $cleanCategory")
        }

        if (errors.isNotEmpty()) {
            CrashReporting.logWarning(TAG, "Card validation failed: $errors")
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            title = cleanTitle,
            body = cleanBody,
            category = cleanCategory,
            errors = errors
        )
    }
}
