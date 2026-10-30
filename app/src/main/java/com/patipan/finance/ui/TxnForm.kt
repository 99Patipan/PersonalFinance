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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.patipan.finance.FinanceVM
import com.patipan.finance.data.*
import java.time.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlin.math.roundToInt
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FormTarget(val txn: Txn?, val type: String)

/** สวิตช์ประเภทรายการ: แผ่นสีเลื่อนไปหาตัวเลือกที่แตะ สีเปลี่ยนตามประเภท (จ่าย=อิฐ รับ=เขียว โอน=หมึก) */
@Composable
private fun TypeSwitch(type: String, onChange: (String) -> Unit) {
    val items = listOf(EXPENSE to "รายจ่าย", INCOME to "รายรับ", TRANSFER to "โอนเงิน")
    val idx = items.indexOfFirst { it.first == type }.coerceAtLeast(0)
    val pillColor by animateColorAsState(
        when (type) { EXPENSE -> ExpenseText; INCOME -> IncomeText; else -> Ink }, tween(220), label = "segColor")
    BoxWithConstraints(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Sand).padding(4.dp)) {
        val cell = maxWidth / 3
        val x by animateDpAsState(cell * idx, spring(dampingRatio = 0.8f, stiffness = 400f), label = "segX")
        Box(Modifier.offset(x = x).width(cell).height(46.dp).clip(RoundedCornerShape(14.dp)).background(pillColor))
        Row(Modifier.fillMaxWidth()) {
            items.forEach { (v, label) ->
                val fg by animateColorAsState(if (v == type) Color.White else Ink, tween(180), label = "segFg")
                Box(Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(14.dp)).pressClickable { onChange(v) },
                    contentAlignment = Alignment.Center) {
                    Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = fg)
                }
            }
        }
    }
}

/**
 * ตารางเลือกหมวดหมู่ แถวละ 4 ช่อง แตะเพื่อเลือก กดค้างแล้วลากเพื่อจัดลำดับ
 * ลำดับที่จัดจะถูกบันทึกขึ้น Supabase และใช้ทุกหน้า
 */
@Composable
private fun CategoryGrid(cats: List<Category>, selectedId: String?, onReorder: (List<String>) -> Unit, onSelect: (String) -> Unit) {
    if (cats.isEmpty()) { EmptyHint("ยังไม่มีหมวดหมู่ พิมพ์ชื่อหมวดใหม่ด้านล่างได้เลย"); return }
    val perRow = 4
    val gap = 8.dp
    val cellH = 82.dp
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val byId = remember(cats) { cats.associateBy { it.id } }
    var order by remember { mutableStateOf(cats.map { it.id }) }
    var dragId by remember { mutableStateOf<String?>(null) }
    var dragPos by remember { mutableStateOf(Offset.Zero) }
    // ข้อมูลจริงเปลี่ยน (เช่น เพิ่ม/ลบหมวด สลับรายรับ-รายจ่าย) และไม่ได้กำลังลากอยู่ ให้ซิงก์ลำดับตาม
    LaunchedEffect(cats) { if (dragId == null) order = cats.map { it.id } }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cellW = (maxWidth - gap * (perRow - 1)) / perRow
        val cellWpx = with(density) { cellW.toPx() }
        val cellHpx = with(density) { cellH.toPx() }
        val gapPx = with(density) { gap.toPx() }
        val rows = (order.size + perRow - 1) / perRow
        fun slot(index: Int) = Offset((index % perRow) * (cellWpx + gapPx), (index / perRow) * (cellHpx + gapPx))

        Box(Modifier.fillMaxWidth().height(cellH * rows + gap * (rows - 1))) {
            order.forEachIndexed { index, id ->
                key(id) {
                    val c = byId[id] ?: return@key
                    val sel = c.id == selectedId
                    val dragging = dragId == id
                    val s = slot(index)
                    val animated by animateIntOffsetAsState(IntOffset(s.x.roundToInt(), s.y.roundToInt()), tween(180), label = "slot")
                    val pos = if (dragging) IntOffset(dragPos.x.roundToInt(), dragPos.y.roundToInt()) else animated
                    Column(Modifier
                        .offset { pos }
                        .size(cellW, cellH)
                        .zIndex(if (dragging) 1f else 0f)
                        .graphicsLayer { scaleX = if (dragging) 1.08f else 1f; scaleY = if (dragging) 1.08f else 1f; alpha = if (dragging) 0.92f else 1f }
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (sel) Mint else Cream)
                        .border(if (sel) 2.dp else 1.dp, if (sel) Moss else Ink.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        .pointerInput(id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    dragId = id
                                    dragPos = slot(order.indexOf(id))
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragPos += amount
                                    val cx = dragPos.x + cellWpx / 2
                                    val cy = dragPos.y + cellHpx / 2
                                    val col = (cx / (cellWpx + gapPx)).toInt().coerceIn(0, perRow - 1)
                                    val row = (cy / (cellHpx + gapPx)).toInt().coerceIn(0, rows - 1)
                                    val target = (row * perRow + col).coerceAtMost(order.lastIndex)
                                    val cur = order.indexOf(id)
                                    if (cur >= 0 && target != cur) {
                                        order = order.toMutableList().also { it.add(target, it.removeAt(cur)) }
                                    }
                                },
                                onDragEnd = {
                                    dragId = null
                                    if (order != cats.map { it.id }) onReorder(order)
                                },
                                onDragCancel = { dragId = null; order = cats.map { it.id } }
                            )
                        }
                        .clickable { onSelect(c.id) }.padding(horizontal = 4.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        CategoryIcon(c.emoji, 32.dp, 24.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(c.name, fontSize = 12.sp, maxLines = 2, textAlign = TextAlign.Center, lineHeight = 14.sp,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, color = Ink)
                    }
                }
            }
        }
    }
    Text("กดค้างที่หมวดหมู่แล้วลากเพื่อจัดลำดับ", fontSize = 11.5.sp, color = InkSoft, modifier = Modifier.padding(start = 2.dp, top = 6.dp))
}

@Composable
fun TxnForm(vm: FinanceVM, target: FormTarget, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val wallets by vm.wallets.collectAsState()
    val cats by vm.categories.collectAsState()
    val init = target.txn

    var type by remember { mutableStateOf(init?.type ?: target.type) }
    var amount by remember { mutableStateOf(init?.let { plain(it.amountCents) } ?: "") }
    var catId by remember { mutableStateOf(init?.categoryId) }
    var customCat by remember { mutableStateOf("") }
    var walletId by remember { mutableStateOf(init?.walletId ?: wallets.firstOrNull()?.id) }
    var toId by remember { mutableStateOf(init?.toWalletId) }
    var dt by remember { mutableStateOf(init?.ldt ?: LocalDateTime.now().withSecond(0).withNano(0)) }
    var detail by remember { mutableStateOf(init?.detail ?: "") }
    var note by remember { mutableStateOf(init?.note ?: "") }
    var receipt by remember { mutableStateOf(init?.receiptPath) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf(false) }
    var showPad by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            uploading = true
            val p = withContext(Dispatchers.IO) { vm.repo.saveReceipt(uri) }
            uploading = false
            if (p != null) receipt = p else error = "แนบรูปไม่สำเร็จ (ตรวจสอบอินเทอร์เน็ตแล้วลองใหม่)"
        }
    }
    val typeCats = cats.filter { it.type == type }
    val bmp = rememberReceipt(receipt)

    fun save() {
        if (saving || uploading) return
        val cents = exprToCents(amount)
        val typed = customCat.trim()
        when {
            cents == null || cents <= 0L -> { error = "กรุณากรอกจำนวนเงินที่มากกว่า 0"; return }
            walletId == null -> { error = "กรุณาเลือกกระเป๋าเงิน"; return }
            type != TRANSFER && catId == null && typed.isEmpty() -> { error = "กรุณาเลือกหรือพิมพ์หมวดหมู่"; return }
            type == TRANSFER && (toId == null || toId == walletId) -> { error = "กรุณาเลือกกระเป๋าปลายทางที่ต่างจากต้นทาง"; return }
        }
        saving = true
        scope.launch {
            try {
                // ถ้าพิมพ์หมวดเอง ให้ใช้ชื่อที่พิมพ์ (สร้างหมวดใหม่อัตโนมัติถ้ายังไม่มี)
                val finalCat: String? = when {
                    type == TRANSFER -> null
                    typed.isNotEmpty() -> vm.repo.findOrCreateCategory(typed, type, if (type == INCOME) IconStore.defaultIncome else IconStore.defaultExpense)
                    else -> catId
                }
                val ok = vm.saveTxn(Txn(
                    id = init?.id ?: "", type = type, amountCents = cents!!,
                    categoryId = finalCat,
                    walletId = walletId!!, toWalletId = if (type == TRANSFER) toId else null,
                    timestamp = dt.toMillis(), detail = detail.trim(), note = note.trim(), receiptPath = receipt
                ))
                if (ok) onClose() else saving = false
            } catch (e: Exception) {
                error = friendlyError(e)
                saving = false
            }
        }
    }

    Surface(Modifier.fillMaxSize(), color = Cream) {
      Column(Modifier.fillMaxSize().gridBackground()) {
        // แถบด้านบนคงที่: ปิด / ชื่อหน้า / ปุ่มบันทึก (ไม่ต้องเลื่อนลงไปกด)
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(Color.White).border(1.dp, Ink.copy(alpha = 0.08f), CircleShape)
                .pressClickable(onClose).semantics { contentDescription = "ปิด" }, contentAlignment = Alignment.Center) {
                LineIcon(Glyph.Close, Ink, Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(if (init == null) "เพิ่มรายการ" else "แก้ไขรายการ", fontSize = 22.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Button(onClick = { save() }, enabled = !saving && !uploading, modifier = Modifier.height(46.dp),
                shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Moss, contentColor = Color.White)) {
                Text("บันทึก", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        error?.let { Text(it, color = ExpenseText, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)) }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TypeSwitch(type) { t -> type = t; catId = null; customCat = "" }
            SoftCard {
                // ช่องจำนวนเงิน: แตะแล้วเปิดแป้นเครื่องคิดเลข (ไม่เปิดคีย์บอร์ดระบบ)
                val result = if (hasOperator(amount)) evalExpr(amount) else null
                Box(Modifier.fillMaxWidth().clickable { showPad = true }) {
                    OutlinedTextField(value = amount, onValueChange = {}, enabled = false,
                        label = { Text("จำนวนเงิน") }, suffix = { Text("บาท") }, singleLine = true,
                        placeholder = { Text("0", style = Num.copy(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)) },
                        supportingText = { if (result != null) Text("= ${money(result.setScale(2, java.math.RoundingMode.HALF_UP).movePointRight(2).toLong())} บาท") },
                        textStyle = Num.copy(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Ink, disabledLabelColor = InkSoft,
                            disabledSuffixColor = Ink, disabledPlaceholderColor = Ink.copy(alpha = 0.3f),
                            disabledSupportingTextColor = IncomeText,
                            disabledBorderColor = if (showPad) Moss else Ink.copy(alpha = 0.38f)),
                        modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(10.dp))
                if (type != TRANSFER) {
                    Text("หมวดหมู่", fontSize = 13.sp, color = InkSoft, modifier = Modifier.padding(start = 2.dp, bottom = 6.dp))
                    CategoryGrid(typeCats, catId, onReorder = { vm.reorderCategories(it) }) { catId = it; customCat = ""; error = null }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(customCat, { customCat = it; if (it.isNotBlank()) catId = null; error = null },
                        label = { Text(if (type == INCOME) "หรือพิมพ์ชื่อหมวดรายรับเอง" else "หรือพิมพ์ว่าจ่ายค่าอะไร (หมวดใหม่)") },
                        placeholder = { Text("เช่น ค่าเทอม, ค่าขนมลูก, ทำบุญ") },
                        supportingText = { Text("พิมพ์แล้วบันทึก ระบบจะเพิ่มเป็นหมวดหมู่ให้อัตโนมัติ") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) showPad = false })
                    Spacer(Modifier.height(10.dp))
                }
                Picker(if (type == TRANSFER) "จากกระเป๋า" else "กระเป๋าเงิน",
                    wallets.firstOrNull { it.id == walletId }?.name ?: "เลือกกระเป๋า", wallets.map { it.name }) {
                    walletId = wallets[it].id; error = null
                }
                if (type == TRANSFER) {
                    Spacer(Modifier.height(10.dp))
                    Picker("ไปยังกระเป๋า", wallets.firstOrNull { it.id == toId }?.name ?: "เลือกกระเป๋า", wallets.map { it.name }) {
                        toId = wallets[it].id; error = null
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pickDate(ctx, dt.toLocalDate()) { d -> dt = LocalDateTime.of(d, dt.toLocalTime()) } },
                        modifier = Modifier.weight(1.5f), shape = RoundedCornerShape(14.dp)) { LineIcon(Glyph.Calendar, Ink, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(dateLabel(dt.toLocalDate()), maxLines = 1) }
                    OutlinedButton(onClick = { pickTime(ctx, dt.toLocalTime()) { t -> dt = LocalDateTime.of(dt.toLocalDate(), t) } },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { LineIcon(Glyph.Clock, Ink, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(timeLabel(dt)) }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(detail, { detail = it }, label = { Text("รายละเอียดเพิ่มเติม") },
                    modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) showPad = false })
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(note, { note = it }, label = { Text("หมายเหตุ") }, minLines = 2,
                    modifier = Modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) showPad = false })
            }
            SoftCard {
                Text("รูปใบเสร็จ (ไม่บังคับ)", fontWeight = FontWeight.Bold)
                if (bmp != null) {
                    Spacer(Modifier.height(8.dp))
                    Image(bmp, "ใบเสร็จ", Modifier.fillMaxWidth().heightIn(max = 220.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { launcher.launch("image/*") }) {
                        LineIcon(Glyph.Camera, Moss, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                        Text(if (uploading) "กำลังอัปโหลดรูป…" else if (receipt == null) "แนบรูป" else "เปลี่ยนรูป", fontWeight = FontWeight.SemiBold)
                    }
                    if (receipt != null) TextButton(onClick = { receipt = null }) { Text("ลบรูป", color = ExpenseText) }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        if (showPad) CalcKeypad(
            onKey = { k -> amount = calcKey(amount, k); error = null },
            onSave = { save() },
            onHide = { showPad = false }
        )
      }
    }
}
