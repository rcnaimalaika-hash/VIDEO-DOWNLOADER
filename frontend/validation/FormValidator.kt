package com.example.videodownloader.util

import android.util.Patterns

/**
 * Result of a single form input validation.
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

/**
 * Password strength levels for user feedback.
 */
enum class PasswordStrength {
    WEAK, MEDIUM, STRONG
}

object FormValidator {

    /**
     * Name validation:
     * - Must not be empty
     * - At least 2 characters
     * - Only letters, spaces, hyphens, and apostrophes
     */
    fun validateName(name: String): ValidationResult {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> ValidationResult(false, "Full Name is required")
            trimmed.length < 2 -> ValidationResult(false, "Name must be at least 2 characters")
            !trimmed.matches(Regex("^[a-zA-Z\\s'-]+$")) -> ValidationResult(false, "Name can only contain letters and spaces")
            else -> ValidationResult(true)
        }
    }

    /**
     * Age validation (Numeric field requirement):
     * - Must not be empty
     * - Must be numeric only
     * - Must be between 13 and 120
     */
    fun validateAge(ageStr: String): ValidationResult {
        val trimmed = ageStr.trim()
        if (trimmed.isEmpty()) {
            return ValidationResult(false, "Age is required")
        }
        val ageNum = trimmed.toIntOrNull()
        return when {
            ageNum == null -> ValidationResult(false, "Age must contain numbers only")
            ageNum < 13 -> ValidationResult(false, "You must be at least 13 years old")
            ageNum > 120 -> ValidationResult(false, "Please enter a valid age (13-120)")
            else -> ValidationResult(true)
        }
    }

    /**
     * Email validation:
     * - Must not be empty
     * - Standard email format
     */
    fun validateEmail(email: String): ValidationResult {
        val trimmed = email.trim()
        return when {
            trimmed.isEmpty() -> ValidationResult(false, "Email address is required")
            !Patterns.EMAIL_ADDRESS.matcher(trimmed).matches() -> ValidationResult(false, "Enter a valid email address (e.g. user@example.com)")
            else -> ValidationResult(true)
        }
    }

    /**
     * Strong Password validation:
     * - Minimum 8 characters
     * - At least 1 uppercase letter
     * - At least 1 lowercase letter
     * - At least 1 number
     * - At least 1 special character (!@#$%^&*...)
     */
    fun validatePassword(password: String): ValidationResult {
        return when {
            password.isEmpty() -> ValidationResult(false, "Password is required")
            password.length < 8 -> ValidationResult(false, "Password must be at least 8 characters")
            !password.any { it.isUpperCase() } -> ValidationResult(false, "Must contain at least 1 uppercase letter (A-Z)")
            !password.any { it.isLowerCase() } -> ValidationResult(false, "Must contain at least 1 lowercase letter (a-z)")
            !password.any { it.isDigit() } -> ValidationResult(false, "Must contain at least 1 number (0-9)")
            !password.any { "!@#$%^&*()_+-=[]{}|;':,./<>?".contains(it) } -> ValidationResult(false, "Must contain at least 1 special character (!@#\$%...)")
            else -> ValidationResult(true)
        }
    }

    /**
     * Evaluates password strength level for dynamic visual meter.
     */
    fun evaluatePasswordStrength(password: String): PasswordStrength {
        if (password.length < 6) return PasswordStrength.WEAK
        var score = 0
        if (password.length >= 8) score++
        if (password.any { it.isUpperCase() }) score++
        if (password.any { it.isLowerCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { "!@#$%^&*()_+-=[]{}|;':,./<>?".contains(it) }) score++

        return when {
            score >= 4 -> PasswordStrength.STRONG
            score >= 2 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.WEAK
        }
    }

    /**
     * Confirm password matching validation.
     */
    fun validateConfirmPassword(password: String, confirmPassword: String): ValidationResult {
        return when {
            confirmPassword.isEmpty() -> ValidationResult(false, "Confirm Password is required")
            password != confirmPassword -> ValidationResult(false, "Passwords do not match")
            else -> ValidationResult(true)
        }
    }

    /**
     * URL validation for video downloader input:
     * - Must not be empty
     * - Must start with http:// or https://
     * - Must have valid domain structure
     */
    fun validateUrl(url: String): ValidationResult {
        val trimmed = url.trim()
        return when {
            trimmed.isEmpty() -> ValidationResult(false, "Please paste or enter a video URL")
            !trimmed.startsWith("http://") && !trimmed.startsWith("https://") ->
                ValidationResult(false, "URL must start with https:// or http://")
            !Patterns.WEB_URL.matcher(trimmed).matches() ->
                ValidationResult(false, "Invalid URL format")
            else -> ValidationResult(true)
        }
    }
}
