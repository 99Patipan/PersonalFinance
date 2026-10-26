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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FormTarget(val txn: Txn?, val type: String)

@Composable
private fun TypeChip(label: String, color: Color, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(14.dp))
        .background(if (selected) color else color.copy(alpha = 0.25f))
        .clickable(onClick = onClick).padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (selected) Color.White else Ink)
    }
}

/** ตารางเลือกหมวดหมู่ เรียงเป็นแถวหน้ากระดาน แถวละ 4 ช่อง */
@Composable
private fun CategoryGrid(cats: List<Category>, selectedId: Long?, onSelect: (Long) -> Unit) {
    if (cats.isEmpty()) { EmptyHint("ยังไม่มีหมวดหมู่ พิมพ์ชื่อหมวดใหม่ด้านล่างได้เลย"); return }
    val perRow = 4
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cats.chunked(perRow).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { c ->
                    val sel = c.id == selectedId
                    Column(Modifier.weight(1f).height(78.dp).clip(RoundedCornerShape(16.dp))
                        .background(if (sel) Yellow else Cream)
                        .border(if (sel) 2.dp else 1.dp, if (sel) Orange else Ink.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        .clickable { onSelect(c.id) }.padding(horizontal = 4.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(c.emoji, fontSize = 24.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(c.name, fontSize = 11.sp, maxLines = 2, textAlign = TextAlign.Center, lineHeight = 13.sp,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, color = Ink)
                    }
                }
                repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
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

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            val p = withContext(Dispatchers.IO) { vm.repo.saveReceipt(uri) }
            if (p != null) receipt = p else error = "แนบรูปไม่สำเร็จ"
        }
    }
    val typeCats = cats.filter { it.type == type }
    val bmp = rememberReceipt(receipt)

    fun save() {
        if (saving) return
        val cents = parseCents(amount)
        val typed = customCat.trim()
        when {
            cents == null || cents <= 0L -> { error = "กรุณากรอกจำนวนเงินที่มากกว่า 0"; return }
            walletId == null -> { error = "กรุณาเลือกกระเป๋าเงิน"; return }
            type != TRANSFER && catId == null && typed.isEmpty() -> { error = "กรุณาเลือกหรือพิมพ์หมวดหมู่"; return }
            type == TRANSFER && (toId == null || toId == walletId) -> { error = "กรุณาเลือกกระเป๋าปลายทางที่ต่างจากต้นทาง"; return }
        }
        saving = true
        scope.launch {
            // ถ้าพิมพ์หมวดเอง ให้ใช้ชื่อที่พิมพ์ (สร้างหมวดใหม่อัตโนมัติถ้ายังไม่มี)
            val finalCat: Long? = when {
                type == TRANSFER -> null
                typed.isNotEmpty() -> vm.repo.findOrCreateCategory(typed, type, if (type == INCOME) "💰" else "🏷️")
                else -> catId
            }
            vm.saveTxn(Txn(
                id = init?.id ?: 0L, type = type, amountCents = cents!!,
                categoryId = finalCat,
                walletId = walletId!!, toWalletId = if (type == TRANSFER) toId else null,
                timestamp = dt.toMillis(), detail = detail.trim(), note = note.trim(), receiptPath = receipt
            ))
            onClose()
        }
    }

    Surface(Modifier.fillMaxSize(), color = Cream) {
      Column(Modifier.fillMaxSize().gridBackground()) {
        // แถบด้านบนคงที่: ปิด / ชื่อหน้า / ปุ่มบันทึก (ไม่ต้องเลื่อนลงไปกด)
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(Color.White).clickable(onClick = onClose), contentAlignment = Alignment.Center) {
                Text("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
            }
            Spacer(Modifier.width(12.dp))
            Text(if (init == null) "เพิ่มรายการ" else "แก้ไขรายการ", fontSize = 22.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Button(onClick = { save() }, enabled = !saving, modifier = Modifier.height(44.dp),
                shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 22.dp)) {
                Text("บันทึก", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        error?.let { Text(it, color = ExpenseRed, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)) }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TypeChip("รายจ่าย", ExpenseRed, type == EXPENSE, Modifier.weight(1f)) { type = EXPENSE; catId = null; customCat = "" }
                TypeChip("รายรับ", IncomeGreen, type == INCOME, Modifier.weight(1f)) { type = INCOME; catId = null; customCat = "" }
                TypeChip("โอนเงิน", Color(0xFF6CB6E8), type == TRANSFER, Modifier.weight(1f)) { type = TRANSFER; catId = null; customCat = "" }
            }
            SoftCard {
                OutlinedTextField(value = amount, onValueChange = { amount = filterAmount(it); error = null },
                    label = { Text("จำนวนเงิน") }, suffix = { Text("บาท") }, singleLine = true,
                    textStyle = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                if (type != TRANSFER) {
                    Text("หมวดหมู่", fontSize = 13.sp, color = Ink.copy(alpha = 0.6f), modifier = Modifier.padding(start = 2.dp, bottom = 6.dp))
                    CategoryGrid(typeCats, catId) { catId = it; customCat = ""; error = null }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(customCat, { customCat = it; if (it.isNotBlank()) catId = null; error = null },
                        label = { Text(if (type == INCOME) "หรือพิมพ์ชื่อหมวดรายรับเอง" else "หรือพิมพ์ว่าจ่ายค่าอะไร (หมวดใหม่)") },
                        placeholder = { Text("เช่น ค่าเทอม, ค่าขนมลูก, ทำบุญ") },
                        supportingText = { Text("พิมพ์แล้วบันทึก ระบบจะเพิ่มเป็นหมวดหมู่ให้อัตโนมัติ") },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
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
                        modifier = Modifier.weight(1.5f)) { Text("📅 ${dateLabel(dt.toLocalDate())}") }
                    OutlinedButton(onClick = { pickTime(ctx, dt.toLocalTime()) { t -> dt = LocalDateTime.of(dt.toLocalDate(), t) } },
                        modifier = Modifier.weight(1f)) { Text("🕒 ${timeLabel(dt)}") }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(detail, { detail = it }, label = { Text("รายละเอียดเพิ่มเติม") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(note, { note = it }, label = { Text("หมายเหตุ") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            }
            SoftCard {
                Text("รูปใบเสร็จ (ไม่บังคับ)", fontWeight = FontWeight.Bold)
                if (bmp != null) {
                    Spacer(Modifier.height(8.dp))
                    Image(bmp, "ใบเสร็จ", Modifier.fillMaxWidth().heightIn(max = 220.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { launcher.launch("image/*") }) { Text(if (bmp == null) "📷 แนบรูป" else "เปลี่ยนรูป") }
                    if (receipt != null) TextButton(onClick = { receipt = null }) { Text("ลบรูป", color = ExpenseRed) }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
      }
    }
}
