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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

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
        // 1) ยอดรวม + กระเป๋า — ตัวเลขใหญ่คือพระเอกของหน้า ไล่ขึ้นครั้งแรกที่เปิดแอป
        item {
            val intro = rememberIntro("hero", 800)
            val shown = (total * intro.toDouble()).toLong()
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = Forest) {
                Box(Modifier.background(Brush.linearGradient(listOf(Forest, ForestLight))).drawBehind {
                    drawCircle(Color.White.copy(alpha = 0.07f), radius = size.width * 0.38f,
                        center = Offset(size.width * 0.96f, size.height * 0.02f))
                    drawCircle(Color.White.copy(alpha = 0.05f), radius = size.width * 0.20f,
                        center = Offset(size.width * 0.78f, size.height * 0.98f))
                }) {
                    Column(Modifier.padding(22.dp)) {
                        Text("ยอดเงินคงเหลือรวม", fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f))
                        Spacer(Modifier.height(2.dp))
                        Text(buildAnnotatedString {
                            withStyle(SpanStyle(fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)) { append(money(shown)) }
                            withStyle(SpanStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold)) { append("  บาท") }
                        }, style = Num, maxLines = 1, color = if (total < 0) Color(0xFFFFD9C7) else Color.White)
                        Text("จาก ${wallets.size} กระเป๋า", fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
                        Spacer(Modifier.height(16.dp))
                        wallets.chunked(2).forEach { row ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { w ->
                                    Row(Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                                        .background(Color.White.copy(alpha = 0.16f)).padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(10.dp).background(Color(w.color), CircleShape))
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text(w.name, fontSize = 13.sp, color = Color.White.copy(alpha = 0.92f), maxLines = 1)
                                            Text(money(walletBalance(w, txns)), style = Num, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
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
                    NavCircle(Glyph.ChevronLeft, "เดือนก่อนหน้า") { vm.month.value = ym.minusMonths(1); vm.selectedDay.value = null }
                    Text(monthLabel(ym), Modifier.weight(1f), textAlign = TextAlign.Center,
                        fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    NavCircle(Glyph.ChevronRight, "เดือนถัดไป") { vm.month.value = ym.plusMonths(1); vm.selectedDay.value = null }
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
        // 3) ธุรกรรมล่าสุด / รายการของวันที่เลือก (การ์ดเดียว หน้าตาเหมือนเดิมเสมอ)
        item {
            val day = sel?.let { s -> txns.filter { it.localDate == s } }
            SoftCard {
                if (sel == null) {
                    SectionTitle("ธุรกรรมล่าสุด") { TextButton(onClick = onSeeAll) { Text("ดูทั้งหมด") } }
                    if (txns.isEmpty()) EmptyHint("ยังไม่มีรายการ กดปุ่ม + เพื่อเพิ่มรายการแรก")
                    txns.take(5).forEach { TxnRow(it, catMap, wMap) { onOpen(it) } }
                } else {
                    SectionTitle("รายการวันที่ ${dateLabel(sel!!)}") {
                        TextButton(onClick = { vm.selectedDay.value = null }) { Text("ล่าสุด") }
                    }
                    if (day.isNullOrEmpty()) EmptyHint("ไม่มีรายการในวันนี้")
                    day?.forEach { TxnRow(it, catMap, wMap, showDate = false) { onOpen(it) } }
                }
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
                    Text("1", fontSize = 12.sp, color = InkSoft); Spacer(Modifier.weight(1f))
                    Text("$n", fontSize = 12.sp, color = InkSoft)
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
                    Box(Modifier.padding(top = 12.dp)) { LineIcon(Glyph.Pencil, Moss, Modifier.size(24.dp)) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(Sand.copy(alpha = 0.7f)).padding(14.dp)) {
                        if (notes.isEmpty()) Text("ยังไม่มีบันทึกในเดือนนี้ (เพิ่มได้ในช่อง หมายเหตุ ของแต่ละรายการ)",
                            fontSize = 14.sp, color = InkSoft)
                        notes.take(10).forEach {
                            Column(Modifier.fillMaxWidth().clickable { onOpen(it) }.padding(vertical = 6.dp)) {
                                Text(it.note, fontSize = 15.sp)
                                Text("${dateLabel(it.localDate)} • ${money(it.amountCents)} บาท", fontSize = 12.sp, color = InkSoft)
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

/** สีพื้นช่องปฏิทิน (ครีมอุ่น) – ปรับสีได้ที่นี่ */
private val CalCell = Color(0xFFFBF1DE)
private val CalCellBorder = Color(0xFFF1E6CC)

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
    Text(t, style = Num, fontSize = dayMoneySize(t), fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false,
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
        thDays.forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = InkSoft) }
    }
    Spacer(Modifier.height(4.dp))
    for (r in 0 until rows) {
        Row(Modifier.fillMaxWidth()) {
            for (c in 0 until 7) {
                val d = r * 7 + c - offset + 1
                if (d < 1 || d > days) {
                    // วันของเดือนก่อน/หลัง แสดงเป็นสีจาง
                    val n = if (d < 1) prevDays + d else d - days
                    Column(Modifier.weight(1f).height(66.dp).padding(1.5.dp).clip(RoundedCornerShape(12.dp))
                        .background(CalCell.copy(alpha = 0.45f)).padding(horizontal = 4.dp, vertical = 4.dp),
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
                    Modifier.weight(1f).height(66.dp).padding(1.5.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (isSel) Mint else CalCell)
                        .border(if (isToday || isSel) 1.5.dp else 1.dp,
                            if (isToday || isSel) Moss else CalCellBorder, RoundedCornerShape(12.dp))
                        .clickable { onSelect(date) }.padding(horizontal = 1.dp, vertical = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("$d", Modifier.fillMaxWidth().padding(end = 3.dp), textAlign = TextAlign.End, fontSize = 11.sp,
                        color = if (isToday || isSel) Moss else InkSoft,
                        fontWeight = if (isToday || isSel) FontWeight.Bold else FontWeight.Normal)
                    Spacer(Modifier.weight(1f))
                    // วันที่มีรายการ: แสดงทั้งรายจ่าย (ส้ม) และรายรับ (เขียว) ฝั่งที่เป็น 0 จะจางลง
                    if (list.isNotEmpty()) {
                        DayAmount(exp, ExpenseText)
                        DayAmount(inc, IncomeText)
                    }
                }
            }
        }
    }
    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).background(ExpenseText, CircleShape))
        Text(" รายจ่าย    ", fontSize = 12.sp, color = InkSoft)
        Box(Modifier.size(7.dp).background(IncomeText, CircleShape))
        Text(" รายรับ    (โอนเงินไม่นับ)", fontSize = 12.sp, color = InkSoft)
    }
}
