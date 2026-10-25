package com.patipan.finance.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.BitmapFactory

val palette = listOf(
    Color(0xFF8DAA93), Color(0xFFF59A62), Color(0xFFB7CBBB), Color(0xFFF9C3A1), Color(0xFF6F9479),
    Color(0xFFD9C7A3), Color(0xFF9FB8C4), Color(0xFFC9A7B8), Color(0xFFB5B58A), Color(0xFF8C9A8F)
)

@Composable
fun SoftCard(modifier: Modifier = Modifier, tint: Color = Color.White, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = tint, shadowElevation = 2.dp) {
        Column(Modifier.padding(18.dp), content = content)
    }
}

@Composable
fun SectionTitle(text: String, trailing: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun Stat(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 3.dp).clip(RoundedCornerShape(20.dp)).background(color.copy(alpha = 0.14f))
        .padding(vertical = 12.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = Ink.copy(alpha = 0.65f))
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
    }
}

@Composable
fun EmptyHint(text: String) {
    Text(text, Modifier.fillMaxWidth().padding(vertical = 18.dp), textAlign = TextAlign.Center,
        color = Ink.copy(alpha = 0.55f), fontSize = 15.sp)
}

@Composable
fun TxnRow(t: Txn, cats: Map<Long, Category>, wallets: Map<Long, Wallet>, showDate: Boolean = true, onClick: () -> Unit) {
    val cat = t.categoryId?.let { cats[it] }
    val emoji = if (t.type == TRANSFER) "🔁" else cat?.emoji ?: "🏷️"
    val title = t.detail.ifBlank { if (t.type == TRANSFER) "โอนเงิน" else cat?.name ?: "ไม่ระบุหมวด" }
    val sub = if (t.type == TRANSFER) "${wallets[t.walletId]?.name ?: "-"} → ${wallets[t.toWalletId]?.name ?: "-"}"
    else "${cat?.name ?: "ไม่ระบุหมวด"} • ${wallets[t.walletId]?.name ?: "-"}"
    val pill = when (t.type) { INCOME -> IncomeGreen; EXPENSE -> Orange; else -> Color(0xFF9AA09B) }
    val circle = when (t.type) { INCOME -> Mint; EXPENSE -> Yellow; else -> Sand }
    val sign = when (t.type) { INCOME -> "+"; EXPENSE -> "-"; else -> "" }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(circle), contentAlignment = Alignment.Center) {
            Text(emoji, fontSize = 22.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1)
            Text(sub, fontSize = 12.sp, color = Ink.copy(alpha = 0.55f), maxLines = 1)
        }
        Column(horizontalAlignment = Alignment.End) {
            Box(Modifier.clip(RoundedCornerShape(12.dp)).background(pill).padding(horizontal = 10.dp, vertical = 5.dp)) {
                Text("$sign${money(t.amountCents)} บาท", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
            }
            Spacer(Modifier.height(2.dp))
            Text(if (showDate) "${dateLabel(t.localDate)} ${timeLabel(t.ldt)}" else timeLabel(t.ldt),
                fontSize = 11.sp, color = Ink.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun DualBars(a: List<Long>, b: List<Long>, modifier: Modifier = Modifier.fillMaxWidth().height(150.dp)) {
    Canvas(modifier) {
        val n = a.size
        if (n == 0) return@Canvas
        val mx = maxOf(a.maxOrNull() ?: 0L, b.maxOrNull() ?: 0L, 1L).toFloat()
        val gw = size.width / n
        val bw = gw * 0.38f
        val r = CornerRadius(minOf(bw / 2f, 4.dp.toPx()))
        for (i in 0 until n) {
            val x = i * gw + gw * 0.08f
            val h1 = a[i] / mx * size.height
            val h2 = b[i] / mx * size.height
            if (h1 > 0f) drawRoundRect(IncomeGreen, Offset(x, size.height - h1), Size(bw, h1), r)
            if (h2 > 0f) drawRoundRect(Orange, Offset(x + bw, size.height - h2), Size(bw, h2), r)
        }
        // แกนซ้าย + เส้นฐาน
        drawLine(Ink.copy(alpha = 0.12f), Offset(0f, 0f), Offset(0f, size.height), 2f)
        drawLine(Ink.copy(alpha = 0.12f), Offset(0f, size.height), Offset(size.width, size.height), 2f)
    }
}

@Composable
fun ChartLegend() {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(IncomeGreen, RoundedCornerShape(3.dp))); Text(" รายรับ     ", fontSize = 12.sp, color = Ink.copy(alpha = 0.7f))
        Box(Modifier.size(10.dp).background(Orange, RoundedCornerShape(3.dp))); Text(" รายจ่าย", fontSize = 12.sp, color = Ink.copy(alpha = 0.7f))
    }
}

@Composable
fun CategoryShare(items: List<Pair<String, Long>>) {
    val total = items.sumOf { it.second }
    if (total <= 0) { EmptyHint("ยังไม่มีรายจ่ายในช่วงนี้"); return }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(170.dp)) {
            val sw = 34.dp.toPx()
            var start = -90f
            items.forEachIndexed { i, p ->
                val sweep = p.second.toFloat() / total * 360f
                drawArc(palette[i % palette.size], start, maxOf(sweep - 1f, 0.5f), false,
                    topLeft = Offset(sw / 2, sw / 2), size = Size(size.width - sw, size.height - sw),
                    style = Stroke(sw))
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("รวม", fontSize = 12.sp, color = Ink.copy(alpha = 0.55f))
            Text(money(total), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1)
        }
    }
    Spacer(Modifier.height(12.dp))
    items.take(8).forEachIndexed { i, p ->
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(16.dp).background(palette[i % palette.size], RoundedCornerShape(5.dp)))
            Spacer(Modifier.width(10.dp))
            Text(p.first, Modifier.weight(1f), fontSize = 15.sp, maxLines = 1)
            Text(String.format(Locale.US, "%.0f%%", p.second * 100.0 / total), fontSize = 13.sp, color = Ink.copy(alpha = 0.55f))
            Spacer(Modifier.width(10.dp))
            Text("${money(p.second)} บาท", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

@Composable
fun Picker(label: String, value: String, options: List<String>, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Surface(onClick = { open = true }, shape = RoundedCornerShape(14.dp), color = Cream,
            border = BorderStroke(1.dp, Ink.copy(alpha = 0.2f)), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(label, fontSize = 11.sp, color = Ink.copy(alpha = 0.6f))
                    Text(value, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
                Text("▾", fontSize = 16.sp)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEachIndexed { i, o ->
                DropdownMenuItem(text = { Text(o) }, onClick = { open = false; onSelect(i) })
            }
        }
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirmLabel: String = "ยืนยัน", onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel, color = ExpenseRed) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ยกเลิก") } })
}

fun pickDate(ctx: Context, initial: LocalDate, onPick: (LocalDate) -> Unit) {
    DatePickerDialog(ctx, { _, y, m, d -> onPick(LocalDate.of(y, m + 1, d)) },
        initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
}

fun pickTime(ctx: Context, initial: LocalTime, onPick: (LocalTime) -> Unit) {
    TimePickerDialog(ctx, { _, h, m -> onPick(LocalTime.of(h, m)) }, initial.hour, initial.minute, true).show()
}

@Composable
fun rememberReceipt(path: String?): ImageBitmap? = remember(path) {
    path?.let { p ->
        runCatching {
            val o = BitmapFactory.Options().apply { inSampleSize = 4 }
            BitmapFactory.decodeFile(p, o)?.asImageBitmap()
        }.getOrNull()
    }
}

@Composable
fun BudgetBar(name: String, used: Long, limit: Long, warnPercent: Int, emoji: String? = null) {
    val pct = if (limit > 0) used * 100.0 / limit else 0.0
    val over = used > limit
    val near = !over && pct >= warnPercent
    val barColor = when { over -> ExpenseRed; near -> Orange; else -> Sage }
    val textColor = when { over -> ExpenseRed; near -> Orange; else -> Ink.copy(alpha = 0.6f) }
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (emoji != null) {
            Box(Modifier.size(46.dp).clip(CircleShape).background(Mint), contentAlignment = Alignment.Center) {
                Text(emoji, fontSize = 22.sp)
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
                Text("${money(used)} / ${money(limit)} บาท", fontSize = 13.sp, color = Ink.copy(alpha = 0.6f))
            }
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)).background(Sand)) {
                Box(Modifier.fillMaxWidth((pct / 100.0).coerceIn(0.0, 1.0).toFloat()).fillMaxHeight()
                    .clip(RoundedCornerShape(7.dp)).background(barColor))
            }
            val remain = limit - used
            Spacer(Modifier.height(3.dp))
            Text(
                when {
                    over -> "⚠️ เกินงบ ${money(-remain)} บาท (ใช้ไป ${String.format(Locale.US, "%.0f", pct)}%)"
                    near -> "⚠️ ใกล้เกินงบ เหลือ ${money(remain)} บาท (ใช้ไป ${String.format(Locale.US, "%.0f", pct)}%)"
                    else -> "เหลือ ${money(remain)} บาท (ใช้ไป ${String.format(Locale.US, "%.0f", pct)}%)"
                }, fontSize = 12.sp, color = textColor
            )
        }
    }
}

@Composable
fun TxnDetailDialog(t: Txn, cats: Map<Long, Category>, wallets: Map<Long, Wallet>,
                    onEdit: () -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    if (confirm) ConfirmDialog("ลบรายการนี้?",
        "รายการจะถูกลบถาวร และยอดคงเหลือของกระเป๋าจะถูกปรับให้ถูกต้อง", "ลบ",
        onConfirm = { confirm = false; onDelete() }, onDismiss = { confirm = false })
    val bmp = rememberReceipt(t.receiptPath)
    val type = when (t.type) { INCOME -> "รายรับ"; EXPENSE -> "รายจ่าย"; else -> "โอนเงิน" }
    @Composable fun line(k: String, v: String) {
        if (v.isNotBlank()) Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            Text(k, Modifier.width(90.dp), color = Ink.copy(alpha = 0.6f)); Text(v, Modifier.weight(1f))
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$type ${money(t.amountCents)} บาท") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                line("วันที่", "${dateLabel(t.localDate)} ${timeLabel(t.ldt)}")
                if (t.type != TRANSFER) line("หมวดหมู่", catName(t.categoryId?.let { cats[it] }))
                line(if (t.type == TRANSFER) "จาก" else "กระเป๋า", wallets[t.walletId]?.name ?: "-")
                if (t.type == TRANSFER) line("ไปยัง", wallets[t.toWalletId]?.name ?: "-")
                line("รายละเอียด", t.detail)
                line("หมายเหตุ", t.note)
                if (bmp != null) {
                    Spacer(Modifier.height(8.dp))
                    Image(bmp, "ใบเสร็จ", Modifier.fillMaxWidth().heightIn(max = 260.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onEdit) { Text("แก้ไข") } },
        dismissButton = {
            Row {
                TextButton(onClick = { confirm = true }) { Text("ลบ", color = ExpenseRed) }
                TextButton(onClick = onDismiss) { Text("ปิด") }
            }
        }
    )
}
