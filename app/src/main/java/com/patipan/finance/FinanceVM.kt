package com.patipan.finance

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.patipan.finance.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class FinanceVM(private val app: Application) : AndroidViewModel(app) {
    val repo = Repository(AppDatabase.get(app), app)
    private val prefs = Prefs(app)

    val wallets = repo.wallets.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val categories = repo.categories.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val txns = repo.txns.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val budgets = repo.budgets.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val warnPercent = prefs.warnPercent.stateIn(viewModelScope, SharingStarted.Eagerly, 80)

    val month = MutableStateFlow(YearMonth.now())
    val selectedDay = MutableStateFlow<LocalDate?>(null)

    private val _msg = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val msg = _msg.asSharedFlow()
    private fun say(s: String) { _msg.tryEmit(s) }

    fun saveTxn(t: Txn) = viewModelScope.launch {
        repo.saveTxn(t); say(if (t.id == 0L) "เพิ่มรายการแล้ว" else "แก้ไขรายการแล้ว")
    }
    fun deleteTxn(t: Txn) = viewModelScope.launch { repo.deleteTxn(t); say("ลบรายการแล้ว") }

    fun saveWallet(w: Wallet) = viewModelScope.launch { repo.saveWallet(w); say("บันทึกกระเป๋าแล้ว") }
    fun deleteWallet(w: Wallet) = viewModelScope.launch {
        say(repo.deleteWallet(w) ?: "ลบกระเป๋าแล้ว")
    }
    fun saveCategory(c: Category) = viewModelScope.launch { repo.saveCategory(c); say("บันทึกหมวดหมู่แล้ว") }
    fun deleteCategory(c: Category) = viewModelScope.launch { repo.deleteCategory(c); say("ลบหมวดหมู่แล้ว") }
    fun setBudget(catId: Long, cents: Long) = viewModelScope.launch { repo.setBudget(catId, cents); say("บันทึกงบประมาณแล้ว") }
    fun setWarn(v: Int) = viewModelScope.launch { prefs.setWarnPercent(v) }

    fun exportCsv(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        try {
            app.contentResolver.openOutputStream(uri, "wt")!!.use { repo.writeCsv(it) }
            say("ส่งออก CSV สำเร็จ")
        } catch (e: Exception) { say("ส่งออกไม่สำเร็จ: ${e.message}") }
    }

    fun backup(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        try {
            app.contentResolver.openOutputStream(uri, "wt")!!.use { repo.writeBackup(it) }
            say("สำรองข้อมูลสำเร็จ")
        } catch (e: Exception) { say("สำรองไม่สำเร็จ: ${e.message}") }
    }

    fun restore(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        try {
            app.contentResolver.openInputStream(uri)!!.use { repo.restoreBackup(it) }
            say("กู้คืนข้อมูลสำเร็จ")
        } catch (e: Exception) { say("กู้คืนไม่สำเร็จ: ${e.message}") }
    }
}
