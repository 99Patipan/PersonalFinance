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
        applyQuickAdd(intent, from = "onCreate")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyQuickAdd(intent, from = "onNewIntent")
    }

    override fun onResume() {
        super.onResume()
        // อ่านซ้ำจาก Prefs เผื่อ Intent extra หาย แต่ Prefs ถูกเซ็ตไว้แล้ว
        val pending = vm.peekPendingQuickAdd()
        if (!pending.isNullOrBlank()) {
            applyQuickAdd(null, from = "onResume/prefs")
        }
    }

    private fun applyQuickAdd(intent: Intent?, from: String) {
        var type: String? = null

        // 1) deep link: personalfinance://quickadd/EXPENSE
        val data = intent?.data
        if (data != null && data.scheme == "personalfinance" && data.host == "quickadd") {
            type = data.lastPathSegment
        }

        // 2) extra
        if (type.isNullOrBlank()) {
            type = intent?.getStringExtra(EXTRA_QUICK_ADD)
        }

        // 3) Prefs (สำรอง)
        if (type.isNullOrBlank()) {
            type = vm.peekPendingQuickAdd()
        }

        if (type.isNullOrBlank()) return
        val normalized = when (type.uppercase()) {
            EXPENSE, "EXPENSE" -> EXPENSE
            INCOME, "INCOME" -> INCOME
            else -> return
        }

        intent?.removeExtra(EXTRA_QUICK_ADD)
        vm.requestQuickAdd(normalized)
    }

    companion object {
        const val EXTRA_QUICK_ADD = "quick_add_type"
    }
}
