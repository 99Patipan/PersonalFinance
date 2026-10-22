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
fun ReportsScreen(vm: FinanceVM) {
    val txns by vm.txns.collectAsState()
    val cats by vm.categories.collectAsState()
    val catMap = remember(cats) { cats.associateBy { it.id } }
    var yearMode by remember { mutableStateOf(false) }
    var ym by remember { mutableStateOf(YearMonth.now()) }
    var year by remember { mutableStateOf(LocalDate.now().year) }
    val today = LocalDate.now()

    val period = remember(txns, yearMode, ym, year) {
        if (yearMode) txns.filter { it.localDate.year == year } else inMonth(txns, ym)
    }
    val income = sumOf(period, INCOME)
    val expense = sumOf(period, EXPENSE)
    val byCat = expenseByCat(period)
    val daysCount = if (yearMode) {
        if (year == today.year) today.dayOfYear else Year.of(year).length()
    } else if (ym == YearMonth.from(today)) today.dayOfMonth else ym.lengthOfMonth()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            SoftCard {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !yearMode, onClick = { yearMode = false }, label = { Text("รายเดือน") })
                    FilterChip(selected = yearMode, onClick = { yearMode = true }, label = { Text("รายปี") })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { if (yearMode) year-- else ym = ym.minusMonths(1) }) { Text("‹", fontSize = 24.sp) }
                    Text(if (yearMode) "ปี ${year + 543}" else monthLabel(ym), Modifier.weight(1f),
                        textAlign = TextAlign.Center, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { if (yearMode) year++ else ym = ym.plusMonths(1) }) { Text("›", fontSize = 24.sp) }
                }
                Row(Modifier.fillMaxWidth()) {
                    Stat("รายรับ", "${money(income)} บาท", IncomeGreen, Modifier.weight(1f))
                    Stat("รายจ่าย", "${money(expense)} บาท", ExpenseRed, Modifier.weight(1f))
                    Stat("สุทธิ", "${money(income - expense)} บาท", if (income < expense) ExpenseRed else Ink, Modifier.weight(1f))
                }
            }
        }
        item {
            SoftCard {
                SectionTitle(if (yearMode) "รายรับ-รายจ่ายรายเดือน" else "รายรับ-รายจ่ายรายวัน")
                Spacer(Modifier.height(8.dp))
                if (yearMode) {
                    val inc = (1..12).map { m -> sumOf(period.filter { it.localDate.monthValue == m }, INCOME) }
                    val exp = (1..12).map { m -> sumOf(period.filter { it.localDate.monthValue == m }, EXPENSE) }
                    DualBars(inc, exp)
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        thMonthsShort.forEach { Text(it, Modifier.weight(1f), fontSize = 8.sp, textAlign = TextAlign.Center, maxLines = 1) }
                    }
                } else {
                    val n = ym.lengthOfMonth()
                    DualBars((1..n).map { d -> sumOf(period.filter { it.localDate.dayOfMonth == d }, INCOME) },
                        (1..n).map { d -> sumOf(period.filter { it.localDate.dayOfMonth == d }, EXPENSE) })
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        Text("1", fontSize = 11.sp); Spacer(Modifier.weight(1f)); Text("$n", fontSize = 11.sp)
                    }
                }
                ChartLegend()
            }
        }
        item {
            SoftCard {
                SectionTitle("ภาพรวมรายจ่าย")
                Spacer(Modifier.height(6.dp))
                val avg = if (daysCount > 0) expense / daysCount else 0L
                Text("ค่าใช้จ่ายเฉลี่ยต่อวัน: ${money(avg)} บาท", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                val top = byCat.firstOrNull()
                Text(
                    if (top != null) "ใช้เงินมากที่สุด: ${catName(top.first?.let { catMap[it] })} (${money(top.second)} บาท)"
                    else "ใช้เงินมากที่สุด: -", fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
                if (yearMode) {
                    val months = if (year == today.year) today.monthValue else 12
                    Text("เฉลี่ยต่อเดือน: ${money(expense / months)} บาท", fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
                } else {
                    val prev = sumOf(inMonth(txns, ym.minusMonths(1)), EXPENSE)
                    val diff = expense - prev
                    val pct = if (prev > 0) String.format(Locale.US, "%.0f%%", abs(diff) * 100.0 / prev) else "-"
                    Spacer(Modifier.height(10.dp))
                    Text("เทียบกับ ${monthLabel(ym.minusMonths(1))}", fontWeight = FontWeight.Bold)
                    Text("เดือนก่อนจ่าย ${money(prev)} บาท", fontSize = 14.sp)
                    Text(
                        when {
                            prev == 0L && expense == 0L -> "ไม่มีรายจ่ายทั้งสองเดือน"
                            diff > 0 -> "เดือนนี้จ่ายมากกว่า ${money(diff)} บาท ($pct)"
                            diff < 0 -> "เดือนนี้จ่ายน้อยกว่า ${money(-diff)} บาท ($pct)"
                            else -> "จ่ายเท่ากับเดือนก่อน"
                        }, fontWeight = FontWeight.Bold, color = if (diff > 0) ExpenseRed else IncomeGreen
                    )
                }
            }
        }
        item {
            SoftCard {
                SectionTitle("สัดส่วนรายจ่ายตามหมวดหมู่")
                Spacer(Modifier.height(8.dp))
                CategoryShare(byCat.map { catName(it.first?.let { id -> catMap[id] }) to it.second })
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}
private fun abs(x: Long) = if (x < 0) -x else x
