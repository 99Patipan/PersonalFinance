package com.patipan.finance

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
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
    }
}
