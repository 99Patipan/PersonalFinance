package com.patipan.finance

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.patipan.finance.data.EXPENSE
import com.patipan.finance.data.INCOME
import com.patipan.finance.ui.FinanceApp
import com.patipan.finance.ui.FinanceTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val vm: FinanceVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            vm.msg.collect { Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show() }
        }
        setContent { FinanceTheme { FinanceApp(vm) } }
        // หลัง setContent แล้วค่อยดึงคำขอจากวิดเจ็ต (Prefs เป็นหลัก)
        consumePendingQuickAdd(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumePendingQuickAdd(intent)
    }

    override fun onResume() {
        super.onResume()
        // กันกรณี activity อยู่แล้ว แล้ว Prefs ถูกเขียนจาก Broadcast ก่อน onNewIntent
        consumePendingQuickAdd(intent)
    }

    /**
     * อ่านคำขอเปิดฟอร์มจาก:
     * 1) Prefs (แหล่งหลัก จาก Broadcast ของวิดเจ็ต)
     * 2) Intent extra / action (สำรอง)
     */
    private fun consumePendingQuickAdd(intent: Intent?) {
        // 1) Prefs
        var type = vm.peekPendingQuickAdd()

        // 2) Intent extra
        if (type.isNullOrBlank() && intent != null) {
            type = intent.getStringExtra(EXTRA_QUICK_ADD)
        }
        // 3) action
        if (type.isNullOrBlank() && intent != null) {
            val action = intent.action.orEmpty()
            type = when {
                action.endsWith("_EXPENSE") || action.contains("EXPENSE") -> EXPENSE
                action.endsWith("_INCOME") || action.contains("INCOME") -> INCOME
                else -> null
            }
        }

        if (type.isNullOrBlank()) return
        if (type != EXPENSE && type != INCOME) return

        intent?.removeExtra(EXTRA_QUICK_ADD)
        vm.requestQuickAdd(type)
    }

    companion object {
        const val EXTRA_QUICK_ADD = "quick_add_type"
    }
}
