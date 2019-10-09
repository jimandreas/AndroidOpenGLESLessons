package com.learnopengles.android.lesson8

interface ErrorHandler {
    enum class ErrorType {
        BUFFER_CREATION_ERROR
    }

    fun handleError(errorType: ErrorType, cause: String)
}