package com.patipan.finance

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.patipan.finance.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

class FinanceVM(private val app: Application) : AndroidViewModel(app) {
    private val api = SupabaseApi(app)
    val repo = Repository(api, app)
    private val prefs = Prefs(app)

    val wallets = repo.wallets
    val categories = repo.categories
    val txns = repo.txns
    val budgets = repo.budgets
    val warnPercent = prefs.warnPercent.stateIn(viewModelScope, SharingStarted.Eagerly, 80)

    val month = MutableStateFlow(YearMonth.now())
    val selectedDay = MutableStateFlow<LocalDate?>(null)

    // ---------- บัญชี / สถานะการโหลด ----------
    val signedIn = api.signedIn
    val loaded = repo.loaded
    val email: String get() = api.email ?: ""
    val lastEmail: String get() = api.lastEmail ?: ""

    private val _authBusy = MutableStateFlow(false)
    val authBusy = _authBusy.asStateFlow()
    private val _authError = MutableStateFlow<String?>(null)
    val authError = _authError.asStateFlow()
    private val _loadError = MutableStateFlow<String?>(null)
    val loadError = _loadError.asStateFlow()

    private val _msg = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val msg = _msg.asSharedFlow()
    private fun say(s: String) { _msg.tryEmit(s) }

    init {
        if (api.signedIn.value) reload()
    }

    /** โหลดข้อมูลทั้งหมดจาก Supabase (ใช้ตอนเปิดแอป เข้าสู่ระบบ และกดรีเฟรช) */
    fun reload() = viewModelScope.launch {
        _loadError.value = null
        try {
            repo.refreshAll()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _loadError.value = friendlyError(e)
        }
    }

    fun refreshNow() = viewModelScope.launch {
        try {
            repo.refreshAll(); say("อัปเดตข้อมูลแล้ว")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            say(friendlyError(e))
        }
    }

    fun signIn(email: String, password: String) {
        if (_authBusy.value) return
        _authBusy.value = true
        _authError.value = null
        viewModelScope.launch {
            var ok = false
            try {
                withContext(Dispatchers.IO) { api.signIn(email, password) }
                ok = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _authError.value = friendlyError(e)
            } finally {
                _authBusy.value = false
            }
            if (ok) reload()
        }
    }

    fun signOut() {
        api.signOut()
        viewModelScope.launch { repo.clear() }
    }

    // ---------- ตัวช่วย: ทำงานแล้วแจ้งผลเป็นข้อความสั้น ๆ ----------
    private fun act(okMsg: String?, block: suspend () -> Unit) = viewModelScope.launch {
        try {
            block()
            if (okMsg != null) say(okMsg)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            say(friendlyError(e))
        }
    }

    /** คืน true เมื่อบันทึกขึ้นเซิร์ฟเวอร์สำเร็จ (ฟอร์มจะปิดเฉพาะเมื่อสำเร็จ) */
    suspend fun saveTxn(t: Txn): Boolean = try {
        repo.saveTxn(t)
        say(if (t.id.isEmpty()) "เพิ่มรายการแล้ว" else "แก้ไขรายการแล้ว")
        true
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        say(friendlyError(e))
        false
    }

    fun deleteTxn(t: Txn) = act("ลบรายการแล้ว") { repo.deleteTxn(t) }

    fun saveWallet(w: Wallet) = act("บันทึกกระเป๋าแล้ว") { repo.saveWallet(w) }
    fun deleteWallet(w: Wallet) = viewModelScope.launch {
        try {
            say(repo.deleteWallet(w) ?: "ลบกระเป๋าแล้ว")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            say(friendlyError(e))
        }
    }
    fun saveCategory(c: Category) = act("บันทึกหมวดหมู่แล้ว") { repo.saveCategory(c) }
    fun reorderCategories(ids: List<String>) = act(null) { repo.reorderCategories(ids) }
    fun deleteCategory(c: Category) = act("ลบหมวดหมู่แล้ว") { repo.deleteCategory(c) }
    fun setBudget(catId: String, cents: Long) = act("บันทึกงบประมาณแล้ว") { repo.setBudget(catId, cents) }
    fun setWarn(v: Int) = viewModelScope.launch { prefs.setWarnPercent(v) }

    fun exportCsv(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        try {
            app.contentResolver.openOutputStream(uri, "wt")!!.use { repo.writeCsv(it) }
            say("ส่งออก CSV สำเร็จ")
        } catch (e: Exception) { say("ส่งออกไม่สำเร็จ: ${friendlyError(e)}") }
    }

    fun backup(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        try {
            app.contentResolver.openOutputStream(uri, "wt")!!.use { repo.writeBackup(it) }
            say("สำรองข้อมูลสำเร็จ")
        } catch (e: Exception) { say("สำรองไม่สำเร็จ: ${friendlyError(e)}") }
    }

    fun restore(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        try {
            app.contentResolver.openInputStream(uri)!!.use { repo.restoreBackup(it) }
            say("กู้คืนข้อมูลสำเร็จ")
        } catch (e: Exception) { say("กู้คืนไม่สำเร็จ: ${friendlyError(e)}") }
    }
}
