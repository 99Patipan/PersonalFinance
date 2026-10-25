package com.patipan.finance.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.patipan.finance.FinanceVM
import com.patipan.finance.data.*
import java.time.*
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
fun FinanceApp(vm: FinanceVM) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var sub by remember { mutableStateOf<String?>(null) }
    var form by remember { mutableStateOf<FormTarget?>(null) }
    var detail by remember { mutableStateOf<Txn?>(null) }
    val cats by vm.categories.collectAsState()
    val wallets by vm.wallets.collectAsState()

    BackHandler(enabled = form != null) { form = null }
    BackHandler(enabled = form == null && detail == null && sub != null) { sub = null }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().gridBackground()) {
            Box(Modifier.weight(1f)) {
                when (sub) {
                    "wallets" -> WalletsScreen(vm, { sub = null }) { form = FormTarget(null, TRANSFER) }
                    "categories" -> CategoriesScreen(vm) { sub = null }
                    "budgets" -> BudgetsScreen(vm) { sub = null }
                    else -> when (tab) {
                        0 -> DashboardScreen(vm, onOpen = { detail = it }, onSeeAll = { tab = 1 })
                        1 -> TransactionsScreen(vm) { detail = it }
                        2 -> ReportsScreen(vm)
                        else -> SettingsScreen(vm) { sub = it }
                    }
                }
            }
            BottomBar(tab, onSelect = { tab = it; sub = null }, onAdd = { form = FormTarget(null, EXPENSE) })
        }

        form?.let { FormHost(vm, it) { form = null } }

        detail?.let { t ->
            val current = vm.txns.collectAsState().value.firstOrNull { it.id == t.id }
            if (current == null) detail = null else TxnDetailDialog(
                current, cats.associateBy { it.id }, wallets.associateBy { it.id },
                onEdit = { form = FormTarget(current, current.type); detail = null },
                onDelete = { vm.deleteTxn(current); detail = null },
                onDismiss = { detail = null })
        }
    }
}

@Composable
private fun FormHost(vm: FinanceVM, target: FormTarget, onClose: () -> Unit) {
    TxnForm(vm, target, onClose)
}

@Composable
private fun BottomBar(selected: Int, onSelect: (Int) -> Unit, onAdd: () -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), shadowElevation = 10.dp) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            NavItem(Glyph.Home, "หน้าหลัก", selected == 0) { onSelect(0) }
            NavItem(Glyph.List, "รายการ", selected == 1) { onSelect(1) }
            Box(Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = onAdd)
                .semantics { contentDescription = "เพิ่มรายการ" }, contentAlignment = Alignment.Center) {
                LineIcon(Glyph.Plus, Orange, Modifier.size(32.dp))
            }
            NavItem(Glyph.Chart, "รายงาน", selected == 2) { onSelect(2) }
            NavItem(Glyph.Gear, "ตั้งค่า", selected == 3) { onSelect(3) }
        }
    }
}

@Composable
private fun RowScope.NavItem(glyph: Glyph, label: String, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick)
        .semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Box(Modifier.size(width = 52.dp, height = 36.dp).clip(RoundedCornerShape(18.dp))
            .background(if (selected) Mint else Color.Transparent), contentAlignment = Alignment.Center) {
            LineIcon(glyph, if (selected) SageDeep else Ink.copy(alpha = 0.4f), Modifier.size(26.dp))
        }
    }
}
