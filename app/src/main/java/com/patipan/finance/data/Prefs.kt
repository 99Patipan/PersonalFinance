package com.patipan.finance.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore("settings")

class Prefs(private val ctx: Context) {
    private val warnKey = intPreferencesKey("warn_percent")
    val warnPercent: Flow<Int> = ctx.dataStore.data.map { it[warnKey] ?: 80 }
    suspend fun setWarnPercent(v: Int) { ctx.dataStore.edit { it[warnKey] = v } }
}
