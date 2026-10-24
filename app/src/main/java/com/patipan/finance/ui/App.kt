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
            Surface(color = Lime, shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp), shadowElevation = 3.dp) {
                Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                        Text("🐥", fontSize = 24.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("สมุดรายรับ-รายจ่าย", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
                        Text("วันนี้ ${dateLabel(LocalDate.now())}", fontSize = 13.sp, color = Ink.copy(alpha = 0.7f))
                    }
                    Text("🐰", fontSize = 26.sp)
                }
            }
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
    Surface(color = Color.White, shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp), shadowElevation = 14.dp) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            NavItem("🏠", "หน้าหลัก", selected == 0) { onSelect(0) }
            NavItem("📒", "รายการ", selected == 1) { onSelect(1) }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(Modifier.size(56.dp).shadow(6.dp, CircleShape).clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFA06B), Orange)))
                    .clickable(onClick = onAdd), contentAlignment = Alignment.Center) {
                    Text("+", fontSize = 32.sp, fontWeight = FontWeight.Medium, color = Color.White)
                }
            }
            NavItem("📊", "รายงาน", selected == 2) { onSelect(2) }
            NavItem("⚙️", "ตั้งค่า", selected == 3) { onSelect(3) }
        }
    }
}

@Composable
private fun RowScope.NavItem(emoji: String, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.clip(RoundedCornerShape(16.dp)).background(if (selected) Lime.copy(alpha = 0.45f) else Color.Transparent)
            .padding(horizontal = 18.dp, vertical = 3.dp)) {
            Text(emoji, fontSize = 22.sp)
        }
        Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Ink else Ink.copy(alpha = 0.6f))
    }
}
