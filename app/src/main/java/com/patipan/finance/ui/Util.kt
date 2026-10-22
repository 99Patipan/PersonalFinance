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
import kotlin.math.abs

val thMonths = listOf("มกราคม", "กุมภาพันธ์", "มีนาคม", "เมษายน", "พฤษภาคม", "มิถุนายน",
    "กรกฎาคม", "สิงหาคม", "กันยายน", "ตุลาคม", "พฤศจิกายน", "ธันวาคม")
val thMonthsShort = listOf("ม.ค.", "ก.พ.", "มี.ค.", "เม.ย.", "พ.ค.", "มิ.ย.",
    "ก.ค.", "ส.ค.", "ก.ย.", "ต.ค.", "พ.ย.", "ธ.ค.")
val thDays = listOf("จ.", "อ.", "พ.", "พฤ.", "ศ.", "ส.", "อา.")

fun money(c: Long): String {
    val a = abs(c)
    val s = if (a % 100 == 0L) String.format(Locale.US, "%,d", a / 100)
    else String.format(Locale.US, "%,d.%02d", a / 100, a % 100)
    return (if (c < 0) "-" else "") + s
}

fun plain(c: Long): String = if (c % 100 == 0L) "${c / 100}" else String.format(Locale.US, "%d.%02d", c / 100, c % 100)

fun compactMoney(c: Long): String {
    val b = c / 100.0
    return if (b >= 10000) String.format(Locale.US, "%.1fk", b / 1000) else String.format(Locale.US, "%,.0f", b)
}

fun monthLabel(ym: YearMonth) = "${thMonths[ym.monthValue - 1]} ${ym.year + 543}"
fun dateLabel(d: LocalDate) = "${thDays[d.dayOfWeek.value - 1]} ${d.dayOfMonth} ${thMonthsShort[d.monthValue - 1]} ${d.year + 543}"
fun timeLabel(t: LocalDateTime) = String.format(Locale.US, "%02d:%02d", t.hour, t.minute)

val Txn.ldt: LocalDateTime get() = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()
val Txn.localDate: LocalDate get() = ldt.toLocalDate()
fun LocalDateTime.toMillis(): Long = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun filterAmount(s: String): String {
    val f = s.filter { it.isDigit() || it == '.' }
    val i = f.indexOf('.')
    if (i < 0) return f.take(10)
    return f.substring(0, i).take(10) + "." + f.substring(i + 1).replace(".", "").take(2)
}

fun parseCents(s: String): Long? = runCatching {
    java.math.BigDecimal(s).setScale(2, java.math.RoundingMode.UNNECESSARY).movePointRight(2).toLong()
}.getOrNull()

fun walletBalance(w: Wallet, all: List<Txn>): Long {
    var b = w.initialCents
    for (x in all) when (x.type) {
        INCOME -> if (x.walletId == w.id) b += x.amountCents
        EXPENSE -> if (x.walletId == w.id) b -= x.amountCents
        TRANSFER -> {
            if (x.walletId == w.id) b -= x.amountCents
            if (x.toWalletId == w.id) b += x.amountCents
        }
    }
    return b
}

fun sumOf(list: List<Txn>, type: String) = list.filter { it.type == type }.sumOf { it.amountCents }
fun inMonth(all: List<Txn>, ym: YearMonth) = all.filter { YearMonth.from(it.localDate) == ym }
fun expenseByCat(list: List<Txn>): List<Pair<Long?, Long>> =
    list.filter { it.type == EXPENSE }.groupBy { it.categoryId }
        .map { it.key to it.value.sumOf { x -> x.amountCents } }.sortedByDescending { it.second }

fun catName(c: Category?) = if (c == null) "ไม่ระบุหมวด" else "${c.emoji} ${c.name}"
