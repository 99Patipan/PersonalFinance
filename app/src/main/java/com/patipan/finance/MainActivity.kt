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
        // เปิดจากวิดเจ็ต (handleQuickAdd จะลบ extra ทิ้งหลังรับแล้ว จึงไม่เปิดซ้ำตอนหมุนจอ)
        handleQuickAdd(intent)
        setContent { FinanceTheme { FinanceApp(vm) } }
    }

    // แอปเปิดค้างอยู่แล้ว แล้วแตะวิดเจ็ต
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleQuickAdd(intent)
    }

    private fun handleQuickAdd(intent: Intent?) {
        if (intent == null) return
        // 1) อ่านจาก extra ก่อน
        var type = intent.getStringExtra(EXTRA_QUICK_ADD)
        // 2) ถ้า extra หาย (พบบ่อยกับวิดเจ็ต + singleTop) ลองอ่านจาก action แทน
        if (type.isNullOrBlank()) {
            val action = intent.action.orEmpty()
            type = when {
                action.endsWith("_EXPENSE") -> EXPENSE
                action.endsWith("_INCOME") -> INCOME
                else -> null
            }
        }
        // 3) ลองจาก data URI
        if (type.isNullOrBlank()) {
            val segment = intent.data?.lastPathSegment
            type = when (segment) {
                EXPENSE, "EXPENSE" -> EXPENSE
                INCOME, "INCOME" -> INCOME
                else -> null
            }
        }
        if (type.isNullOrBlank()) return
        intent.removeExtra(EXTRA_QUICK_ADD)
        if (type == EXPENSE || type == INCOME) vm.requestQuickAdd(type)
    }

    companion object { const val EXTRA_QUICK_ADD = "quick_add_type" }
}
