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

    private val ui = ctx.getSharedPreferences("ui_state", Context.MODE_PRIVATE)
    /** กระเป๋าที่เลือกล่าสุดในหน้าเพิ่มรายการ (อ่านแบบ sync เพื่อใช้เป็นค่าเริ่มต้นของฟอร์มทันที) */
    fun lastWalletId(): String? = ui.getString("last_wallet_id", null)
    fun setLastWalletId(id: String) { ui.edit().putString("last_wallet_id", id).apply() }
}
