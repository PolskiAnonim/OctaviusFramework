package org.octavius.form.component

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf

class ErrorManager {
    private val _globalErrors = mutableStateOf<List<String>>(emptyList())
    val globalErrors = _globalErrors

    private val _fieldErrors = mutableStateMapOf<String, List<String>>()
    val fieldErrors = _fieldErrors

    fun addGlobalError(error: String) {
        _globalErrors.value = _globalErrors.value + error
    }

    fun addGlobalErrors(errors: List<String>) {
        _globalErrors.value = _globalErrors.value + errors
    }

    fun addFieldError(fieldName: String, error: String) {
        val currentErrors = _fieldErrors[fieldName] ?: emptyList()
        _fieldErrors[fieldName] = currentErrors + error
    }

    fun addFieldErrors(fieldName: String, errors: List<String>) {
        val currentErrors = _fieldErrors[fieldName] ?: emptyList()
        _fieldErrors[fieldName] = currentErrors + errors
    }

    fun setFieldErrors(fieldName: String, errors: List<String>) {
        if (errors.isEmpty()) {
            _fieldErrors.remove(fieldName)
        } else {
            _fieldErrors[fieldName] = errors
        }
    }

    fun clearGlobalErrors() {
        _globalErrors.value = emptyList()
    }

    fun clearFieldErrors() {
        _fieldErrors.clear()
    }

    fun clearFieldErrors(fieldName: String) {
        _fieldErrors.remove(fieldName)
    }

    fun clearAll() {
        clearGlobalErrors()
        clearFieldErrors()
    }

    fun hasErrors(): Boolean {
        return _globalErrors.value.isNotEmpty() || _fieldErrors.isNotEmpty()
    }

    fun hasFieldErrors(): Boolean {
        return _fieldErrors.isNotEmpty()
    }

    fun hasFieldErrors(fieldName: String): Boolean {
        return _fieldErrors[fieldName]?.isNotEmpty() == true
    }

    fun getFieldErrors(fieldName: String): List<String> {
        return _fieldErrors[fieldName] ?: emptyList()
    }
}