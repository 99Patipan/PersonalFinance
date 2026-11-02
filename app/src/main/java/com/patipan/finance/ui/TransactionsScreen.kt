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
import java.util.Locale

@Composable
fun TransactionsScreen(vm: FinanceVM, onOpen: (Txn) -> Unit) {
    val ctx = LocalContext.current
    val wallets by vm.wallets.collectAsState()
    val txns by vm.txns.collectAsState()
    val cats by vm.categories.collectAsState()
    val catMap = remember(cats) { cats.associateBy { it.id } }
    val wMap = remember(wallets) { wallets.associateBy { it.id } }

    var query by remember { mutableStateOf("") }
    var typeF by remember { mutableStateOf<String?>(null) }
    var catF by remember { mutableStateOf<String?>(null) }
    var from by remember { mutableStateOf<LocalDate?>(null) }
    var to by remember { mutableStateOf<LocalDate?>(null) }

    val filtered = remember(txns, query, typeF, catF, from, to, catMap, wMap) {
        val q = query.trim().lowercase()
        txns.filter { t ->
            (typeF == null || t.type == typeF) &&
                (catF == null || t.categoryId == catF) &&
                (from == null || !t.localDate.isBefore(from)) &&
                (to == null || !t.localDate.isAfter(to)) &&
                (q.isEmpty() || t.detail.lowercase().contains(q) || t.note.lowercase().contains(q) ||
                    (catMap[t.categoryId]?.name?.lowercase()?.contains(q) == true) ||
                    (wMap[t.walletId]?.name?.lowercase()?.contains(q) == true) ||
                    plain(t.amountCents).contains(q))
        }
    }
    val grouped = filtered.groupBy { it.localDate }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { ScreenTitle("รายการ") }
        item {
            SoftCard {
                OutlinedTextField(query, { query = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { LineIcon(Glyph.Search, InkSoft, Modifier.size(22.dp)) },
                    label = { Text("ค้นหา (รายละเอียด หมายเหตุ หมวด จำนวน)") })
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf<Pair<String?, String>>(null to "ทั้งหมด", INCOME to "รายรับ", EXPENSE to "รายจ่าย", TRANSFER to "โอน").forEach { (v, l) ->
                        Pill(l, typeF == v) { typeF = v }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Picker("หมวดหมู่", catF?.let { catName(catMap[it]) } ?: "ทุกหมวดหมู่",
                    listOf("ทุกหมวดหมู่") + cats.map { "${it.label()} (${if (it.type == INCOME) "รับ" else "จ่าย"})" }) {
                    catF = if (it == 0) null else cats[it - 1].id
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pickDate(ctx, from ?: LocalDate.now()) { from = it } }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                        Text(from?.let { "จาก ${dateLabel(it)}" } ?: "จากวันที่", maxLines = 1, fontSize = 13.sp)
                    }
                    OutlinedButton(onClick = { pickDate(ctx, to ?: LocalDate.now()) { to = it } }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                        Text(to?.let { "ถึง ${dateLabel(it)}" } ?: "ถึงวันที่", maxLines = 1, fontSize = 13.sp)
                    }
                }
                if (from != null || to != null || catF != null || typeF != null || query.isNotEmpty()) {
                    TextButton(onClick = { query = ""; typeF = null; catF = null; from = null; to = null }) { Text("ล้างตัวกรอง") }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Stat("รายรับ", money(sumOf(filtered, INCOME)), IncomeGreen, Modifier.weight(1f))
                    Stat("รายจ่าย", money(sumOf(filtered, EXPENSE)), ExpenseRed, Modifier.weight(1f))
                    Stat("พบ", "${filtered.size} รายการ", Ink, Modifier.weight(1f))
                }
            }
        }
        if (filtered.isEmpty()) item { SoftCard { EmptyHint(if (txns.isEmpty()) "ยังไม่มีรายการ กดปุ่ม + เพื่อเพิ่ม" else "ไม่พบรายการที่ตรงกับตัวกรอง") } }
        grouped.forEach { (day, list) ->
            item(key = day.toString()) {
                SoftCard {
                    Row {
                        Text(dateLabel(day), Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        val net = sumOf(list, INCOME) - sumOf(list, EXPENSE)
                        Text("สุทธิ ${money(net)} บาท", style = Num, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            color = if (net < 0) ExpenseText else IncomeText)
                    }
                    list.forEach { TxnRow(it, catMap, wMap, showDate = false) { onOpen(it) } }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}
