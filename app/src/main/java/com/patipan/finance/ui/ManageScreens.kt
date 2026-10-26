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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

private val walletColors = listOf(
    0xFFFFC857L, 0xFFF58FB0L, 0xFF7BC47FL, 0xFF6CB6E8L, 0xFFFF8A4CL, 0xFFB59AE8L,
    0xFF4DB6AC, 0xFFE57373, 0xFF9CCC65, 0xFF90A4AE, 0xFFFFB74D, 0xFF7986CB
).map { it and 0xFFFFFFFFL }

/** ชุดไอคอนหมวดหมู่ แยกเป็นกลุ่มให้เลือกง่าย ครอบคลุมค่าใช้จ่ายในชีวิตประจำวัน */
private val emojiGroups = listOf(
    "🍽️ อาหารและเครื่องดื่ม" to listOf("🍜", "🍚", "🍔", "🍕", "🍱", "🥗", "☕", "🧋", "🍰", "🍺", "🛒", "🍎"),
    "🚗 เดินทาง" to listOf("🚌", "🚇", "🚕", "🚗", "⛽", "🛵", "🚲", "🅿️", "🛣️", "✈️", "🚢", "🔧"),
    "🏠 บ้านและบิล" to listOf("🏠", "💡", "💧", "📱", "🌐", "📺", "🛋️", "🧹", "🧺", "🔑", "🔥", "📮"),
    "🛍️ ช้อปปิ้งและความงาม" to listOf("🛍️", "👕", "👟", "👜", "🧴", "💄", "💇", "⌚", "💍", "🎒", "🕶️", "📦"),
    "💊 สุขภาพและการเรียน" to listOf("💊", "🏥", "🦷", "🩺", "🏋️", "🧘", "🎓", "📚", "✏️", "💻", "👓", "🧪"),
    "🎮 บันเทิงและสังคม" to listOf("🎮", "🎬", "🎵", "🎤", "🎉", "🎁", "🏖️", "📷", "⚽", "🎨", "🍻", "🎟️"),
    "👨‍👩‍👧 ครอบครัวและสัตว์เลี้ยง" to listOf("👶", "🍼", "🧸", "👨‍👩‍👧", "👵", "🐶", "🐱", "🐟", "🌱", "💐", "🙏", "🕯️"),
    "💰 การเงินและอื่น ๆ" to listOf("💰", "💵", "🪙", "💳", "🏦", "📈", "📊", "🧾", "🛡️", "🤝", "⏰", "💼", "✨", "🏆", "🎯", "🏷️")
)

@Composable
private fun PageHeader(title: String, onBack: () -> Unit, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(Color.White).border(1.dp, Ink.copy(alpha = 0.08f), CircleShape)
            .pressClickable(onBack).semantics { contentDescription = "ย้อนกลับ" }, contentAlignment = Alignment.Center) {
            LineIcon(Glyph.ChevronLeft, Ink, Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(title, Modifier.weight(1f), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        action?.invoke()
    }
}

@Composable
private fun AddPill(onClick: () -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(50)).background(Moss).pressClickable(onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        LineIcon(Glyph.Plus, Color.White, Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("เพิ่ม", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

@Composable
private fun RoundAction(bg: Color, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.size(40.dp).clip(CircleShape).background(bg).pressClickable(onClick), contentAlignment = Alignment.Center) { content() }
}

/** เดาไอคอนของกระเป๋าจากชื่อ (ไม่ต้องแก้ฐานข้อมูล) */
private fun walletEmoji(name: String): String {
    val n = name.lowercase()
    fun has(vararg k: String) = k.any { n.contains(it) }
    return when {
        has("เครดิต", "บัตร", "credit", "card") -> "💳"
        has("ธนาคาร", "บัญชี", "bank", "กสิกร", "ไทยพาณิชย์", "กรุงเทพ", "กรุงไทย", "กรุงศรี", "ออมสิน", "scb", "kbank", "ktb", "bbl", "ttb") -> "🏦"
        has("ออม", "saving") -> "🐷"
        has("สด", "cash") -> "💵"
        has("ทอง", "gold") -> "🪙"
        has("ลงทุน", "หุ้น", "กองทุน", "คริปโต", "invest", "crypto") -> "📈"
        has("พร้อมเพย์", "pay", "wallet", "วอลเล็ต", "truemoney", "ทรู", "line") -> "📱"
        else -> "👛"
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
        PageHeader("กระเป๋าเงิน", onBack) { AddPill { adding = true } }
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                val total = wallets.sumOf { walletBalance(it, txns) }
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = Forest) {
                    Box(Modifier.background(Brush.linearGradient(listOf(Forest, ForestLight))).padding(22.dp)) {
                        Column(Modifier.align(Alignment.CenterStart)) {
                            Text("ยอดรวมทุกกระเป๋า", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
                            Spacer(Modifier.height(4.dp))
                            Text("${money(total)} บาท", style = Num, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.height(6.dp))
                            Box(Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.22f)).padding(horizontal = 10.dp, vertical = 3.dp)) {
                                Text("${wallets.size} กระเป๋า", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                        LineIcon(Glyph.Wallet, Color.White.copy(alpha = 0.3f), Modifier.size(76.dp).align(Alignment.CenterEnd))
                    }
                }
            }
            item {
                Button(onClick = onTransfer, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Action, contentColor = Color.White)) {
                    LineIcon(Glyph.Transfer, Color.White, Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("โอนเงินระหว่างกระเป๋า", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Text("การโอนไม่ถูกนับเป็นรายรับหรือรายจ่ายรวม", fontSize = 12.sp, color = InkSoft,
                    modifier = Modifier.padding(start = 6.dp, top = 6.dp))
            }
            items(wallets, key = { it.id }) { w ->
                val c = Color(w.color)
                val bal = walletBalance(w, txns)
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = Color.White, border = BorderStroke(1.dp, Ink.copy(alpha = 0.07f))) {
                    Row(Modifier.height(IntrinsicSize.Min)) {
                        Box(Modifier.width(9.dp).fillMaxHeight().background(c))
                        Row(Modifier.weight(1f).padding(start = 14.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(54.dp).clip(CircleShape).background(c.copy(alpha = 0.28f)).border(2.dp, c.copy(alpha = 0.7f), CircleShape),
                                contentAlignment = Alignment.Center) { Text(walletEmoji(w.name), fontSize = 26.sp) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(w.name, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1)
                                Text("เริ่มต้น ${money(w.initialCents)} บาท", fontSize = 12.sp, color = InkSoft)
                                Spacer(Modifier.height(2.dp))
                                Text("${money(bal)} บาท", style = Num, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp,
                                    color = if (bal < 0) ExpenseText else Moss)
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                RoundAction(Sand, { edit = w }) { LineIcon(Glyph.Pencil, Ink, Modifier.size(20.dp)) }
                                RoundAction(Pink, { del = w }) { LineIcon(Glyph.Trash, ExpenseText, Modifier.size(20.dp)) }
                            }
                        }
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
    AlertDialog(onDismissRequest = onDismiss, shape = RoundedCornerShape(28.dp), containerColor = Color.White,
        title = { Text(if (w == null) "เพิ่มกระเป๋า" else "แก้ไขกระเป๋า", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(64.dp).align(Alignment.CenterHorizontally).clip(CircleShape).background(Color(color).copy(alpha = 0.3f))
                    .border(2.dp, Color(color), CircleShape), contentAlignment = Alignment.Center) {
                    Text(walletEmoji(name), fontSize = 32.sp)
                }
                OutlinedTextField(name, { name = it }, label = { Text("ชื่อกระเป๋า") }, singleLine = true, shape = RoundedCornerShape(16.dp))
                Text("สีประจำกระเป๋า", fontSize = 13.sp, color = InkSoft)
                walletColors.chunked(6).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { c ->
                            Box(Modifier.size(34.dp).clip(CircleShape).background(Color(c))
                                .border(if (c == color) 3.dp else 0.dp, Ink, CircleShape).clickable { color = c })
                        }
                    }
                }
                OutlinedTextField(init, { init = filterAmount(it) }, label = { Text("ยอดเงินเริ่มต้น (บาท)") }, singleLine = true,
                    shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cents = parseCents(init.ifBlank { "0" })
                if (name.isNotBlank() && cents != null) onSave(Wallet(w?.id ?: 0L, name.trim(), color, cents))
            }) { Text("บันทึก", fontWeight = FontWeight.Bold) }
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
        PageHeader("หมวดหมู่", onBack) { AddPill { adding = true } }
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("รายจ่าย", type == EXPENSE) { type = EXPENSE }
            Pill("รายรับ", type == INCOME) { type = INCOME }
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(cats.filter { it.type == type }, key = { it.id }) { c ->
                SoftCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(if (type == EXPENSE) Pink else Mint), contentAlignment = Alignment.Center) {
                            Text(c.emoji, fontSize = 22.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(c.name, Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.Medium)
                        RoundAction(Sand, { edit = c }) { LineIcon(Glyph.Pencil, Ink, Modifier.size(20.dp)) }
                        Spacer(Modifier.width(8.dp))
                        RoundAction(Pink, { del = c }) { LineIcon(Glyph.Trash, ExpenseText, Modifier.size(20.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryDialog(c: Category?, type: String, onSave: (Category) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(c?.name ?: "") }
    var emoji by remember { mutableStateOf(c?.emoji ?: emojiGroups[0].second[0]) }
    AlertDialog(onDismissRequest = onDismiss, shape = RoundedCornerShape(28.dp), containerColor = Color.White,
        title = { Text(if (c == null) "เพิ่มหมวดหมู่" else "แก้ไขหมวดหมู่", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(56.dp).clip(CircleShape).background(Mint), contentAlignment = Alignment.Center) { Text(emoji, fontSize = 28.sp) }
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(name, { name = it }, label = { Text("ชื่อหมวดหมู่") }, singleLine = true, shape = RoundedCornerShape(16.dp))
                }
                OutlinedTextField(emoji, { v -> emoji = firstSymbol(v).ifEmpty { "🏷️" } },
                    label = { Text("พิมพ์อีโมจิเองก็ได้") }, singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
                emojiGroups.forEach { (title, list) ->
                    Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = InkSoft, modifier = Modifier.padding(top = 6.dp))
                    list.chunked(6).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { e ->
                                Box(Modifier.size(40.dp).clip(CircleShape)
                                    .background(if (e == emoji) Mint else Cream)
                                    .border(if (e == emoji) 2.dp else 0.dp, Moss, CircleShape).clickable { emoji = e },
                                    contentAlignment = Alignment.Center) { Text(e, fontSize = 20.sp) }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) onSave(Category(c?.id ?: 0L, name.trim(), c?.type ?: type, emoji))
            }) { Text("บันทึก", fontWeight = FontWeight.Bold) }
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
                    if (bMap[c.id] != null) TextButton(onClick = { vm.setBudget(c.id, 0); edit = null }) { Text("ลบงบ", color = ExpenseText) }
                    TextButton(onClick = { edit = null }) { Text("ยกเลิก") }
                }
            })
    }
    Column(Modifier.fillMaxSize()) {
        PageHeader("งบประมาณรายเดือน", onBack)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("แตะที่หมวดหมู่เพื่อตั้งหรือแก้ไขงบ (นับจากรายจ่ายของเดือนปัจจุบัน)", fontSize = 13.sp, color = InkSoft) }
            items(cats.filter { it.type == EXPENSE }, key = { it.id }) { c ->
                val b = bMap[c.id]
                SoftCard(Modifier.clickable { edit = c }) {
                    if (b == null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${c.emoji} ${c.name}", Modifier.weight(1f), fontSize = 17.sp, fontWeight = FontWeight.Medium)
                            Text("ยังไม่ตั้งงบ", color = InkSoft, fontSize = 13.sp)
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

/** ตัดเอาตัวอักษร/อีโมจิตัวแรกเพียงตัวเดียว */
private fun firstSymbol(v: String): String {
    val t = v.trim()
    if (t.isEmpty()) return ""
    val it = java.text.BreakIterator.getCharacterInstance()
    it.setText(t)
    return t.substring(0, it.next())
}
