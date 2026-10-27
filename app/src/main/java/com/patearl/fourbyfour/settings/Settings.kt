package com.patearl.fourbyfour.settings

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** User settings backed by SharedPreferences. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _maxHr = MutableStateFlow(prefs.getInt(KEY_MAX_HR, DEFAULT_MAX_HR))
    val maxHr: StateFlow<Int> = _maxHr.asStateFlow()

    fun setMaxHr(value: Int) {
        val clamped = value.coerceIn(MAX_HR_RANGE)
        _maxHr.value = clamped
        prefs.edit { putInt(KEY_MAX_HR, clamped) }
    }

    companion object {
        const val DEFAULT_MAX_HR = 190
        val MAX_HR_RANGE = 120..230
        private const val KEY_MAX_HR = "max_hr"
    }
}
