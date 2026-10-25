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
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = Forest, shadowElevation = 3.dp) {
                Box(Modifier.background(Brush.linearGradient(listOf(Forest, ForestLight)))) {
                    Column(Modifier.padding(22.dp)) {
                        Text("ยอดเงินคงเหลือรวม", fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f))
                        Text("${money(total)} บาท", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold,
                            color = if (total < 0) Color(0xFFFFD9C7) else Color.White)
                        Text("จาก ${wallets.size} กระเป๋า", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                        Spacer(Modifier.height(16.dp))
                        wallets.chunked(2).forEach { row ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { w ->
                                    Row(Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                                        .background(Color.White.copy(alpha = 0.18f)).padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(10.dp).background(Color(w.color), CircleShape))
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text(w.name, fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f), maxLines = 1)
                                            Text(money(walletBalance(w, txns)), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                                        }
                                    }
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        // 2) สรุปเดือน + ปฏิทิน
        item {
            SoftCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { vm.month.value = ym.minusMonths(1); vm.selectedDay.value = null }) {
                        Text("‹", fontSize = 24.sp, color = Ink.copy(alpha = 0.5f))
                    }
                    Surface(Modifier.weight(1f), shape = RoundedCornerShape(50), color = Sand) {
                        Text(monthLabel(ym), Modifier.fillMaxWidth().padding(vertical = 8.dp), textAlign = TextAlign.Center,
                            fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = { vm.month.value = ym.plusMonths(1); vm.selectedDay.value = null }) {
                        Text("›", fontSize = 24.sp, color = Ink.copy(alpha = 0.5f))
                    }
                }
                if (ym != YearMonth.now()) TextButton(onClick = { vm.month.value = YearMonth.now(); vm.selectedDay.value = null },
                    modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("กลับเดือนปัจจุบัน") }
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                    Stat("รายจ่ายรวม", "${money(expense)} บาท", ExpenseRed, Modifier.weight(1f))
                    Stat("รายรับรวม", "${money(income)} บาท", IncomeGreen, Modifier.weight(1f))
                    Stat("ยอดสุทธิ", "${money(income - expense)} บาท", if (income - expense < 0) ExpenseRed else Ink, Modifier.weight(1f))
                }
                CalendarGrid(ym, monthTx, sel) { d -> vm.selectedDay.value = if (sel == d) null else d }
            }
        }
        if (sel != null) item {
            val day = txns.filter { it.localDate == sel }
            SoftCard(tint = Yellow.copy(alpha = 0.6f)) {
                Text("รายการวันที่ ${dateLabel(sel!!)}", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
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
                    Text("1", fontSize = 11.sp, color = Ink.copy(alpha = 0.6f)); Spacer(Modifier.weight(1f))
                    Text("$n", fontSize = 11.sp, color = Ink.copy(alpha = 0.6f))
                }
                ChartLegend()
            }
        }
        // 5) สัดส่วนค่าใช้จ่าย (โดนัท)
        item {
            SoftCard {
                SectionTitle("ค่าใช้จ่ายตามหมวดหมู่")
                Spacer(Modifier.height(6.dp))
                CategoryShare(expenseByCat(monthTx).map { catName(it.first?.let { id -> catMap[id] }) to it.second })
            }
        }
        // 6) เป้าหมายงบประมาณ
        item {
            SoftCard {
                SectionTitle("เป้าหมายงบประมาณ")
                if (budgets.isEmpty()) EmptyHint("ยังไม่ได้ตั้งงบประมาณ ไปที่ ตั้งค่า > งบประมาณ")
                budgets.forEach { b ->
                    val c = catMap[b.categoryId] ?: return@forEach
                    val used = monthTx.filter { it.type == EXPENSE && it.categoryId == b.categoryId }.sumOf { it.amountCents }
                    BudgetBar(c.name, used, b.limitCents, warn, emoji = c.emoji)
                }
            }
        }
        // 7) บันทึกของฉัน
        item {
            val notes = monthTx.filter { it.note.isNotBlank() }
            SoftCard {
                SectionTitle("บันทึกของฉัน")
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Box(Modifier.padding(top = 12.dp)) { LineIcon(Glyph.Pencil, SageDeep, Modifier.size(24.dp)) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(Sand.copy(alpha = 0.7f)).padding(14.dp)) {
                        if (notes.isEmpty()) Text("ยังไม่มีบันทึกในเดือนนี้ (เพิ่มได้ในช่อง หมายเหตุ ของแต่ละรายการ)",
                            fontSize = 14.sp, color = Ink.copy(alpha = 0.55f))
                        notes.take(10).forEach {
                            Column(Modifier.fillMaxWidth().clickable { onOpen(it) }.padding(vertical = 6.dp)) {
                                Text(it.note, fontSize = 15.sp)
                                Text("${dateLabel(it.localDate)} • ${money(it.amountCents)} บาท", fontSize = 12.sp, color = Ink.copy(alpha = 0.55f))
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
fun CalendarGrid(ym: YearMonth, monthTx: List<Txn>, selected: LocalDate?, onSelect: (LocalDate) -> Unit) {
    val byDay = remember(monthTx) { monthTx.filter { it.type != TRANSFER }.groupBy { it.localDate } }
    val offset = ym.atDay(1).dayOfWeek.value - 1
    val days = ym.lengthOfMonth()
    val rows = (offset + days + 6) / 7
    val today = LocalDate.now()
    Row(Modifier.fillMaxWidth()) {
        thDays.forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ink.copy(alpha = 0.5f)) }
    }
    Spacer(Modifier.height(4.dp))
    for (r in 0 until rows) {
        Row(Modifier.fillMaxWidth()) {
            for (c in 0 until 7) {
                val d = r * 7 + c - offset + 1
                if (d < 1 || d > days) { Spacer(Modifier.weight(1f).height(52.dp)); continue }
                val date = ym.atDay(d)
                val list = byDay[date].orEmpty()
                val inc = sumOf(list, INCOME)
                val exp = sumOf(list, EXPENSE)
                val isSel = date == selected
                val isToday = date == today
                Column(
                    Modifier.weight(1f).height(52.dp).padding(1.5.dp).clip(RoundedCornerShape(14.dp))
                        .background(if (isSel) Yellow else Color.Transparent)
                        .then(if (isToday || isSel) Modifier.border(1.5.dp, Orange, RoundedCornerShape(14.dp)) else Modifier)
                        .clickable { onSelect(date) }.padding(vertical = 4.dp, horizontal = 1.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("$d", fontSize = 12.sp, color = if (isToday) Orange else Ink.copy(alpha = 0.65f),
                        fontWeight = if (isToday || isSel) FontWeight.Bold else FontWeight.Normal)
                    Spacer(Modifier.height(3.dp))
                    if (isSel) {
                        if (exp > 0) Text(compactMoney(exp), fontSize = 10.sp, color = ExpenseRed, fontWeight = FontWeight.Bold, maxLines = 1)
                        if (inc > 0) Text(compactMoney(inc), fontSize = 10.sp, color = IncomeGreen, fontWeight = FontWeight.Bold, maxLines = 1)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            if (exp > 0) Box(Modifier.size(7.dp).background(Orange, CircleShape))
                            if (inc > 0) Box(Modifier.size(7.dp).background(Sage, CircleShape))
                        }
                    }
                }
            }
        }
    }
    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).background(Orange, CircleShape))
        Text(" รายจ่าย    ", fontSize = 11.sp, color = Ink.copy(alpha = 0.55f))
        Box(Modifier.size(7.dp).background(Sage, CircleShape))
        Text(" รายรับ    (โอนเงินไม่นับ • แตะวันที่เพื่อดูยอด)", fontSize = 11.sp, color = Ink.copy(alpha = 0.55f))
    }
}
