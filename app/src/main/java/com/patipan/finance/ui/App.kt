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
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween

@Composable
fun FinanceApp(vm: FinanceVM) {
    val signedIn by vm.signedIn.collectAsState()
    val loaded by vm.loaded.collectAsState()
    CompositionLocalProvider(LocalRepo provides vm.repo) {
        when {
            !signedIn -> LoginScreen(vm)
            !loaded -> LoadingScreen(vm)
            else -> MainContent(vm)
        }
    }
}

@Composable
private fun MainContent(vm: FinanceVM) {
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
                // เปลี่ยนหน้าด้วยการค่อย ๆ จางสลับ (200ms) ใช้ key เป็นตัวตัดสิน ไม่อ่านค่า tab ตอนกำลังสลับ
                Crossfade(targetState = sub ?: "tab$tab", animationSpec = tween(200), label = "screen") { key ->
                    when (key) {
                        "wallets" -> WalletsScreen(vm, { sub = null }) { form = FormTarget(null, TRANSFER) }
                        "categories" -> CategoriesScreen(vm) { sub = null }
                        "budgets" -> BudgetsScreen(vm) { sub = null }
                        "tab1" -> TransactionsScreen(vm) { detail = it }
                        "tab2" -> ReportsScreen(vm)
                        "tab3" -> SettingsScreen(vm) { sub = it }
                        else -> DashboardScreen(vm, onOpen = { detail = it }, onSeeAll = { tab = 1 })
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
    Surface(color = Color.White, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), shadowElevation = 8.dp) {
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            NavItem(Glyph.Home, "หน้าหลัก", selected == 0) { onSelect(0) }
            NavItem(Glyph.List, "รายการ", selected == 1) { onSelect(1) }
            // ปุ่มหลัก: ช่องสี่เหลี่ยมมนสีส้ม ไอคอนบวกสีขาว เด่นกว่าทุกปุ่มในแถบ
            Box(Modifier.weight(1f).height(60.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(54.dp).shadow(6.dp, RoundedCornerShape(20.dp)).clip(RoundedCornerShape(20.dp))
                    .background(Action).pressClickable(onAdd).semantics { contentDescription = "เพิ่มรายการ" },
                    contentAlignment = Alignment.Center) {
                    LineIcon(Glyph.Plus, Color.White, Modifier.size(28.dp))
                }
            }
            NavItem(Glyph.Chart, "รายงาน", selected == 2) { onSelect(2) }
            NavItem(Glyph.Gear, "ตั้งค่า", selected == 3) { onSelect(3) }
        }
    }
}

@Composable
private fun RowScope.NavItem(glyph: Glyph, label: String, selected: Boolean, onClick: () -> Unit) {
    val tint by animateColorAsState(if (selected) Moss else InkSoft, tween(200), label = "navTint")
    val pill by animateColorAsState(if (selected) Mint else Color.Transparent, tween(200), label = "navPill")
    Column(Modifier.weight(1f).height(60.dp).clip(RoundedCornerShape(18.dp)).pressClickable(onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(width = 56.dp, height = 30.dp).clip(RoundedCornerShape(15.dp)).background(pill),
            contentAlignment = Alignment.Center) {
            LineIcon(glyph, tint, Modifier.size(22.dp))
        }
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = tint, maxLines = 1)
    }
}
