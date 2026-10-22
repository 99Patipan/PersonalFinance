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
        when {
            cents == null || cents <= 0L -> { error = "กรุณากรอกจำนวนเงินที่มากกว่า 0"; return }
            walletId == null -> { error = "กรุณาเลือกกระเป๋าเงิน"; return }
            type != TRANSFER && catId == null -> { error = "กรุณาเลือกหมวดหมู่"; return }
            type == TRANSFER && (toId == null || toId == walletId) -> { error = "กรุณาเลือกกระเป๋าปลายทางที่ต่างจากต้นทาง"; return }
        }
        saving = true
        vm.saveTxn(Txn(
            id = init?.id ?: 0L, type = type, amountCents = cents!!,
            categoryId = if (type == TRANSFER) null else catId,
            walletId = walletId!!, toWalletId = if (type == TRANSFER) toId else null,
            timestamp = dt.toMillis(), detail = detail.trim(), note = note.trim(), receiptPath = receipt
        ))
        onClose()
    }

    Surface(Modifier.fillMaxSize(), color = Cream) {
        Column(Modifier.fillMaxSize().gridBackground().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (init == null) "เพิ่มรายการ" else "แก้ไขรายการ", fontSize = 24.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onClose) { Text("✕ ปิด", fontSize = 16.sp) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TypeChip("รายจ่าย", ExpenseRed, type == EXPENSE, Modifier.weight(1f)) { type = EXPENSE; catId = null }
                TypeChip("รายรับ", IncomeGreen, type == INCOME, Modifier.weight(1f)) { type = INCOME; catId = null }
                TypeChip("โอนเงิน", Color(0xFF6CB6E8), type == TRANSFER, Modifier.weight(1f)) { type = TRANSFER; catId = null }
            }
            SoftCard {
                OutlinedTextField(value = amount, onValueChange = { amount = filterAmount(it); error = null },
                    label = { Text("จำนวนเงิน") }, suffix = { Text("บาท") }, singleLine = true,
                    textStyle = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                if (type != TRANSFER) {
                    Picker("หมวดหมู่", catName(typeCats.firstOrNull { it.id == catId }).takeIf { catId != null } ?: "เลือกหมวดหมู่",
                        typeCats.map { "${it.emoji} ${it.name}" }) { catId = typeCats[it].id; error = null }
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
            error?.let { Text(it, color = ExpenseRed, fontWeight = FontWeight.Bold) }
            Button(onClick = { save() }, enabled = !saving, modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp)) { Text("บันทึกรายการ", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(24.dp))
        }
    }
}
