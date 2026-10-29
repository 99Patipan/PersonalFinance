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
fun DashboardScreen(vm: FinanceVM, onOpen: (Txn) -> Unit, onSeeAll: () -> Unit) {
    val wallets by vm.wallets.collectAsState()
    val txns by vm.txns.collectAsState()
    val cats by vm.categories.collectAsState()
    val budgets by vm.budgets.collectAsState()
    val warn by vm.warnPercent.collectAsState()
    val ym by vm.month.collectAsState()
    val sel by vm.selectedDay.collectAsState()
    val catMap = remember(cats) { cats.associateBy { it.id } }
    val wMap = remember(wallets) { wallets.associateBy { it.id } }
    val monthTx = remember(txns, ym) { inMonth(txns, ym) }
    val income = sumOf(monthTx, INCOME)
    val expense = sumOf(monthTx, EXPENSE)
    val total = wallets.sumOf { walletBalance(it, txns) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 1) ยอดรวม + กระเป๋า
        item {
            SoftCard {
                Text("ยอดเงินคงเหลือรวม (จาก ${wallets.size} กระเป๋า)", fontSize = 14.sp, color = Ink.copy(alpha = 0.7f))
                Text("${money(total)} บาท", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (total < 0) ExpenseRed else Ink)
                Spacer(Modifier.height(10.dp))
                wallets.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { w ->
                            Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                                .background(Color(w.color).copy(alpha = 0.3f)).padding(12.dp)) {
                                Text("👛 ${w.name}", fontSize = 14.sp, maxLines = 1)
                                Text(money(walletBalance(w, txns)), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        // 2) สรุปเดือน + ปฏิทิน
        item {
            SoftCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { vm.month.value = ym.minusMonths(1); vm.selectedDay.value = null }) { Text("‹", fontSize = 24.sp) }
                    Text(monthLabel(ym), Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { vm.month.value = ym.plusMonths(1); vm.selectedDay.value = null }) { Text("›", fontSize = 24.sp) }
                }
                if (ym != YearMonth.now()) TextButton(onClick = { vm.month.value = YearMonth.now(); vm.selectedDay.value = null },
                    modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("กลับเดือนปัจจุบัน") }
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Stat("รายจ่ายรวม", "${money(expense)} บาท", ExpenseRed, Modifier.weight(1f))
                    Stat("รายรับรวม", "${money(income)} บาท", IncomeGreen, Modifier.weight(1f))
                    Stat("ยอดสุทธิ", "${money(income - expense)} บาท", if (income - expense < 0) ExpenseRed else Ink, Modifier.weight(1f))
                }
                CalendarGrid(ym, monthTx, sel) { d -> vm.selectedDay.value = if (sel == d) null else d }
            }
        }
        if (sel != null) item {
            val day = txns.filter { it.localDate == sel }
            SoftCard(tint = Yellow.copy(alpha = 0.25f)) {
                Text("รายการวันที่ ${dateLabel(sel!!)}", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                if (day.isEmpty()) EmptyHint("ไม่มีรายการในวันนี้")
                day.forEach { TxnRow(it, catMap, wMap, showDate = false) { onOpen(it) } }
            }
        }
        // 3) ธุรกรรมล่าสุด
        item {
            SoftCard {
                SectionTitle("ธุรกรรมล่าสุด") { TextButton(onClick = onSeeAll) { Text("ดูทั้งหมด") } }
                if (txns.isEmpty()) EmptyHint("ยังไม่มีรายการ กดปุ่ม + เพื่อเพิ่มรายการแรก")
                txns.take(5).forEach { TxnRow(it, catMap, wMap) { onOpen(it) } }
            }
        }
        // 4) กราฟรายวัน
        item {
            SoftCard {
                SectionTitle("กราฟรายรับ-รายจ่าย ${thMonths[ym.monthValue - 1]}")
                val n = ym.lengthOfMonth()
                val inc = (1..n).map { d -> sumOf(monthTx.filter { it.localDate.dayOfMonth == d }, INCOME) }
                val exp = (1..n).map { d -> sumOf(monthTx.filter { it.localDate.dayOfMonth == d }, EXPENSE) }
                Spacer(Modifier.height(8.dp))
                DualBars(inc, exp)
                Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text("1", fontSize = 11.sp); Spacer(Modifier.weight(1f)); Text("$n", fontSize = 11.sp)
                }
                ChartLegend()
            }
        }
        // 5) สัดส่วนตามหมวดหมู่
        item {
            SoftCard {
                SectionTitle("รายจ่ายตามหมวดหมู่")
                Text("${money(expense)} บาท", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                Spacer(Modifier.height(8.dp))
                CategoryShare(expenseByCat(monthTx).map { catName(it.first?.let { id -> catMap[id] }) to it.second })
            }
        }
        // 6) งบประมาณ
        item {
            SoftCard {
                SectionTitle("งบประมาณเดือนนี้")
                if (budgets.isEmpty()) EmptyHint("ยังไม่ได้ตั้งงบประมาณ ไปที่ ตั้งค่า > งบประมาณ")
                budgets.forEach { b ->
                    val c = catMap[b.categoryId] ?: return@forEach
                    val used = monthTx.filter { it.type == EXPENSE && it.categoryId == b.categoryId }.sumOf { it.amountCents }
                    BudgetBar("${c.emoji} ${c.name}", used, b.limitCents, warn)
                }
            }
        }
        // 7) บันทึกของฉัน
        item {
            val notes = monthTx.filter { it.note.isNotBlank() }
            SoftCard(tint = Pink.copy(alpha = 0.25f)) {
                SectionTitle("📝 บันทึกของฉัน")
                if (notes.isEmpty()) EmptyHint("ยังไม่มีบันทึกในเดือนนี้ (เพิ่มได้ในช่อง หมายเหตุ ของแต่ละรายการ)")
                notes.take(10).forEach {
                    Column(Modifier.fillMaxWidth().clickable { onOpen(it) }.padding(vertical = 6.dp)) {
                        Text(it.note, fontSize = 15.sp)
                        Text("${dateLabel(it.localDate)} • ${money(it.amountCents)} บาท", fontSize = 12.sp, color = Ink.copy(alpha = 0.6f))
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

/** ยอดเงินในช่องปฏิทิน: เต็มจำนวนมีคอมมา (ต่ำกว่า 100 บาทแสดงสตางค์ถ้ามี) */
private fun dayMoney(c: Long): String =
    if (c < 10000L) money(c) else String.format(Locale.US, "%,d", Math.round(c / 100.0))

private fun dayMoneySize(t: String) = when {
    t.length <= 4 -> 12.sp
    t.length == 5 -> 11.sp
    t.length == 6 -> 10.sp
    else -> 8.5.sp
}

@Composable
private fun DayAmount(cents: Long, color: Color) {
    val t = dayMoney(cents)
    Text(t, fontSize = dayMoneySize(t), fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false,
        color = if (cents > 0) color else color.copy(alpha = 0.3f))
}

@Composable
fun CalendarGrid(ym: YearMonth, monthTx: List<Txn>, selected: LocalDate?, onSelect: (LocalDate) -> Unit) {
    val byDay = remember(monthTx) { monthTx.filter { it.type != TRANSFER }.groupBy { it.localDate } }
    val offset = ym.atDay(1).dayOfWeek.value - 1          // จันทร์ = 0
    val days = ym.lengthOfMonth()
    val prevDays = ym.minusMonths(1).lengthOfMonth()
    val rows = (offset + days + 6) / 7
    val today = LocalDate.now()
    Row(Modifier.fillMaxWidth()) {
        thDays.forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
    }
    Spacer(Modifier.height(4.dp))
    for (r in 0 until rows) {
        Row(Modifier.fillMaxWidth()) {
            for (c in 0 until 7) {
                val d = r * 7 + c - offset + 1
                if (d < 1 || d > days) {
                    // วันของเดือนก่อน/หลัง แสดงเป็นสีจาง
                    val n = if (d < 1) prevDays + d else d - days
                    Column(Modifier.weight(1f).height(66.dp).padding(1.5.dp).clip(RoundedCornerShape(10.dp))
                        .background(Cream.copy(alpha = 0.35f)).padding(horizontal = 3.dp, vertical = 2.dp),
                        horizontalAlignment = Alignment.End) {
                        Text("$n", fontSize = 11.sp, color = Ink.copy(alpha = 0.25f))
                    }
                    continue
                }
                val date = ym.atDay(d)
                val list = byDay[date].orEmpty()
                val inc = sumOf(list, INCOME)
                val exp = sumOf(list, EXPENSE)
                val isSel = date == selected
                val isToday = date == today
                Column(
                    Modifier.weight(1f).height(66.dp).padding(1.5.dp).clip(RoundedCornerShape(10.dp))
                        .background(if (isSel) Yellow else Cream.copy(alpha = 0.7f))
                        .clickable { onSelect(date) }.padding(horizontal = 1.dp, vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (isToday) Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFE5B400)))
                        Spacer(Modifier.weight(1f))
                        Text("$d", fontSize = 11.sp, color = Ink.copy(alpha = if (isToday) 0.9f else 0.55f),
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                    }
                    Spacer(Modifier.weight(1f))
                    // วันที่มีรายการ: แสดงทั้งรายจ่าย (แดง) และรายรับ (เขียว) ฝั่งที่เป็น 0 จะจางลง
                    if (list.isNotEmpty()) {
                        DayAmount(exp, ExpenseRed)
                        DayAmount(inc, IncomeGreen)
                    }
                }
            }
        }
    }
    Text("แดง = รายจ่าย  เขียว = รายรับ  (โอนเงินไม่นับ)", fontSize = 11.sp, color = Ink.copy(alpha = 0.55f),
        modifier = Modifier.padding(top = 6.dp))
}
