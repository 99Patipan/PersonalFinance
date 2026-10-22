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

private val walletColors = listOf(0xFFFFC857L, 0xFFF58FB0L, 0xFF7BC47FL, 0xFF6CB6E8L, 0xFFFF8A4CL, 0xFFB59AE8L)
private val emojiChoices = listOf("🍜", "🚌", "⛽", "🏠", "💡", "📱", "🛍️", "💊", "🎮", "💳", "📦", "💰",
    "⏰", "🎁", "💼", "🏦", "✨", "☕", "🐶", "🎓", "✈️", "👶", "🧴", "🍰")

@Composable
private fun PageHeader(title: String, onBack: () -> Unit, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack) { Text("‹ กลับ", fontSize = 16.sp) }
        Text(title, Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        action?.invoke()
    }
}

// ---------------- กระเป๋าเงิน ----------------
@Composable
fun WalletsScreen(vm: FinanceVM, onBack: () -> Unit, onTransfer: () -> Unit) {
    val wallets by vm.wallets.collectAsState()
    val txns by vm.txns.collectAsState()
    var edit by remember { mutableStateOf<Wallet?>(null) }
    var adding by remember { mutableStateOf(false) }
    var del by remember { mutableStateOf<Wallet?>(null) }

    if (adding || edit != null) WalletDialog(edit, { vm.saveWallet(it); adding = false; edit = null }, { adding = false; edit = null })
    del?.let { w ->
        ConfirmDialog("ลบกระเป๋า \"${w.name}\"?", "ลบได้เฉพาะกระเป๋าที่ไม่มีรายการใช้งานอยู่", "ลบ",
            onConfirm = { vm.deleteWallet(w); del = null }, onDismiss = { del = null })
    }
    Column(Modifier.fillMaxSize()) {
        PageHeader("กระเป๋าเงิน", onBack) { TextButton(onClick = { adding = true }) { Text("+ เพิ่ม", fontSize = 16.sp) } }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Button(onClick = onTransfer, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Text("🔁 โอนเงินระหว่างกระเป๋า")
                }
                Text("การโอนไม่ถูกนับเป็นรายรับหรือรายจ่ายรวม", fontSize = 12.sp, color = Ink.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp))
            }
            items(wallets, key = { it.id }) { w ->
                SoftCard(tint = Color(w.color).copy(alpha = 0.25f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("👛 ${w.name}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("เริ่มต้น ${money(w.initialCents)} บาท", fontSize = 12.sp, color = Ink.copy(alpha = 0.6f))
                            Text("คงเหลือ ${money(walletBalance(w, txns))} บาท", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                        TextButton(onClick = { edit = w }) { Text("แก้ไข") }
                        TextButton(onClick = { del = w }) { Text("ลบ", color = ExpenseRed) }
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletDialog(w: Wallet?, onSave: (Wallet) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(w?.name ?: "") }
    var color by remember { mutableStateOf(w?.color ?: walletColors[0]) }
    var init by remember { mutableStateOf(w?.let { plain(it.initialCents) } ?: "0") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (w == null) "เพิ่มกระเป๋า" else "แก้ไขกระเป๋า") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("ชื่อกระเป๋า") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    walletColors.forEach { c ->
                        Box(Modifier.size(36.dp).clip(CircleShape).background(Color(c))
                            .border(if (c == color) 3.dp else 0.dp, Ink, CircleShape).clickable { color = c })
                    }
                }
                OutlinedTextField(init, { init = filterAmount(it) }, label = { Text("ยอดเงินเริ่มต้น (บาท)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cents = parseCents(init.ifBlank { "0" })
                if (name.isNotBlank() && cents != null) onSave(Wallet(w?.id ?: 0L, name.trim(), color, cents))
            }) { Text("บันทึก") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ยกเลิก") } })
}

// ---------------- หมวดหมู่ ----------------
@Composable
fun CategoriesScreen(vm: FinanceVM, onBack: () -> Unit) {
    val cats by vm.categories.collectAsState()
    var type by remember { mutableStateOf(EXPENSE) }
    var edit by remember { mutableStateOf<Category?>(null) }
    var adding by remember { mutableStateOf(false) }
    var del by remember { mutableStateOf<Category?>(null) }

    if (adding || edit != null) CategoryDialog(edit, type, { vm.saveCategory(it); adding = false; edit = null }, { adding = false; edit = null })
    del?.let { c ->
        ConfirmDialog("ลบหมวด \"${c.name}\"?", "รายการเดิมในหมวดนี้จะกลายเป็น \"ไม่ระบุหมวด\" และงบประมาณของหมวดนี้จะถูกลบ", "ลบ",
            onConfirm = { vm.deleteCategory(c); del = null }, onDismiss = { del = null })
    }
    Column(Modifier.fillMaxSize()) {
        PageHeader("หมวดหมู่", onBack) { TextButton(onClick = { adding = true }) { Text("+ เพิ่ม", fontSize = 16.sp) } }
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = type == EXPENSE, onClick = { type = EXPENSE }, label = { Text("รายจ่าย") })
            FilterChip(selected = type == INCOME, onClick = { type = INCOME }, label = { Text("รายรับ") })
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(cats.filter { it.type == type }, key = { it.id }) { c ->
                SoftCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(c.emoji, fontSize = 26.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(c.name, Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.Medium)
                        TextButton(onClick = { edit = c }) { Text("แก้ไข") }
                        TextButton(onClick = { del = c }) { Text("ลบ", color = ExpenseRed) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryDialog(c: Category?, type: String, onSave: (Category) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(c?.name ?: "") }
    var emoji by remember { mutableStateOf(c?.emoji ?: emojiChoices[0]) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (c == null) "เพิ่มหมวดหมู่" else "แก้ไขหมวดหมู่") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("ชื่อหมวดหมู่") }, singleLine = true)
                emojiChoices.chunked(6).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { e ->
                            Box(Modifier.size(40.dp).clip(CircleShape)
                                .background(if (e == emoji) Yellow else Cream).clickable { emoji = e },
                                contentAlignment = Alignment.Center) { Text(e, fontSize = 20.sp) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) onSave(Category(c?.id ?: 0L, name.trim(), c?.type ?: type, emoji))
            }) { Text("บันทึก") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ยกเลิก") } })
}

// ---------------- งบประมาณ ----------------
@Composable
fun BudgetsScreen(vm: FinanceVM, onBack: () -> Unit) {
    val cats by vm.categories.collectAsState()
    val budgets by vm.budgets.collectAsState()
    val txns by vm.txns.collectAsState()
    val warn by vm.warnPercent.collectAsState()
    var edit by remember { mutableStateOf<Category?>(null) }
    val monthTx = remember(txns) { inMonth(txns, YearMonth.now()) }
    val bMap = budgets.associateBy { it.categoryId }

    edit?.let { c ->
        var text by remember(c.id) { mutableStateOf(bMap[c.id]?.let { plain(it.limitCents) } ?: "") }
        AlertDialog(onDismissRequest = { edit = null }, title = { Text("งบ ${c.emoji} ${c.name} ต่อเดือน") },
            text = {
                OutlinedTextField(text, { text = filterAmount(it) }, label = { Text("งบประมาณ (บาท)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            },
            confirmButton = {
                TextButton(onClick = {
                    val cents = parseCents(text.ifBlank { "0" })
                    if (cents != null) { vm.setBudget(c.id, cents); edit = null }
                }) { Text("บันทึก") }
            },
            dismissButton = {
                Row {
                    if (bMap[c.id] != null) TextButton(onClick = { vm.setBudget(c.id, 0); edit = null }) { Text("ลบงบ", color = ExpenseRed) }
                    TextButton(onClick = { edit = null }) { Text("ยกเลิก") }
                }
            })
    }
    Column(Modifier.fillMaxSize()) {
        PageHeader("งบประมาณรายเดือน", onBack)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("แตะที่หมวดหมู่เพื่อตั้งหรือแก้ไขงบ (นับจากรายจ่ายของเดือนปัจจุบัน)", fontSize = 13.sp, color = Ink.copy(alpha = 0.7f)) }
            items(cats.filter { it.type == EXPENSE }, key = { it.id }) { c ->
                val b = bMap[c.id]
                SoftCard(Modifier.clickable { edit = c }) {
                    if (b == null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${c.emoji} ${c.name}", Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.Medium)
                            Text("ยังไม่ตั้งงบ", color = Ink.copy(alpha = 0.5f), fontSize = 13.sp)
                        }
                    } else {
                        val used = monthTx.filter { it.type == EXPENSE && it.categoryId == c.id }.sumOf { it.amountCents }
                        BudgetBar("${c.emoji} ${c.name}", used, b.limitCents, warn)
                    }
                }
            }
        }
    }
}
