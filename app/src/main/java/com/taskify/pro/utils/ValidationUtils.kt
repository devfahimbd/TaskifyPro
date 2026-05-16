package com.taskify.pro.utils

/**
 * Centralised form-validation helpers.
 *
 * Every method returns null when the input is valid, or a human-readable
 * error string that can be shown directly in a SnackBar / TextInputLayout.
 */
object ValidationUtils {

    /** Minimum password length enforced client-side. */
    private const val MIN_PASSWORD_LENGTH = 6

    /**
     * Validate an email string.
     * Uses Android's built-in pattern which covers ~99 % of valid addresses.
     */
    fun validateEmail(email: String): String? {
        return when {
            email.isBlank() -> "Email cannot be empty"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                "Please enter a valid email address"
            else -> null
        }
    }

    /**
     * Validate a password.
     * Currently checks minimum length. Extend with complexity rules as needed.
     */
    fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> "Password cannot be empty"
            password.length < MIN_PASSWORD_LENGTH ->
                "Password must be at least $MIN_PASSWORD_LENGTH characters"
            else -> null
        }
    }

    /**
     * Validate a confirm-password field matches the original password.
     */
    fun validateConfirmPassword(password: String, confirmPassword: String): String? {
        return when {
            confirmPassword.isBlank() -> "Please confirm your password"
            confirmPassword != password -> "Passwords do not match"
            else -> null
        }
    }

    /**
     * Validate task title — the only mandatory field on a Task.
     */
    fun validateTaskTitle(title: String): String? {
        return when {
            title.isBlank() -> "Task title is required"
            title.length > 200 -> "Title must be 200 characters or fewer"
            else -> null
        }
    }

    /**
     * Validate optional task description.
     */
    fun validateTaskDescription(description: String): String? {
        return if (description.length > 1000) {
            "Description must be 1000 characters or fewer"
        } else {
            null
        }
    }
}
