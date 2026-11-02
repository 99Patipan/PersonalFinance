package com.patipan.finance.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

private const val OPS = "+-×÷"

/** คำนวณสูตรเช่น "12+3×4" (คูณหารก่อนบวกลบ) คืน null ถ้าสูตรไม่ถูกต้อง */
fun evalExpr(src: String): BigDecimal? {
    val s = src.trimEnd { it in OPS || it == '.' }
    if (s.isEmpty()) return null
    val nums = mutableListOf<BigDecimal>()
    val ops = mutableListOf<Char>()
    var i = 0
    while (true) {
        val st = i
        if (i < s.length && s[i] == '-') i++
        while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
        nums += s.substring(st, i).toBigDecimalOrNull() ?: return null
        if (i >= s.length) break
        if (s[i] !in OPS) return null
        ops += s[i]; i++
    }
    val vals = mutableListOf(nums[0])
    val addOps = mutableListOf<Char>()
    for (k in ops.indices) {
        val n = nums[k + 1]
        when (ops[k]) {
            '×' -> vals[vals.lastIndex] = vals.last().multiply(n)
            '÷' -> {
                if (n.signum() == 0) return null
                vals[vals.lastIndex] = vals.last().divide(n, MathContext.DECIMAL64)
            }
            else -> { addOps += ops[k]; vals += n }
        }
    }
    var r = vals[0]
    for (k in addOps.indices) r = if (addOps[k] == '+') r.add(vals[k + 1]) else r.subtract(vals[k + 1])
    return r
}

fun fmtCalc(r: BigDecimal): String =
    r.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

/** จำนวนเงิน (สตางค์) จากสูตร – null ถ้าสูตรไม่ถูกต้อง */
fun exprToCents(s: String): Long? = evalExpr(s)?.setScale(2, RoundingMode.HALF_UP)?.movePointRight(2)?.toLong()

fun hasOperator(s: String) = s.drop(1).any { it in OPS }

/** ผลของการกดปุ่ม [k] บนแป้นเครื่องคิดเลข เมื่อสูตรปัจจุบันคือ [e] */
fun calcKey(e: String, k: String): String {
    val cur = e.takeLastWhile { it.isDigit() || it == '.' }
    return when (k) {
        "C" -> ""
        "⌫" -> e.dropLast(1)
        "=" -> evalExpr(e)?.let { fmtCalc(it) } ?: e
        "+", "-", "×", "÷" -> when {
            e.isEmpty() -> if (k == "-") "-" else e
            e.last() in OPS -> if (e.length == 1) e else e.dropLast(1) + k
            e.last() == '.' -> e.dropLast(1) + k
            else -> e + k
        }
        "." -> when {
            '.' in cur -> e
            cur.isEmpty() -> e + "0."
            else -> e + "."
        }
        else -> {
            val dot = cur.indexOf('.')
            when {
                cur == "0" -> e.dropLast(1) + k
                dot >= 0 && cur.length - dot - 1 >= 2 -> e
                cur.length >= 10 -> e
                else -> e + k
            }
        }
    }
}

private val KeyBg = Color(0xFFEEF3EC)
private val KeyBorder = Color(0xFFDDE6DB)

@Composable
private fun PadKey(modifier: Modifier, bg: Color = KeyBg, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier.height(54.dp).clip(RoundedCornerShape(50)).background(bg)
            .border(1.dp, KeyBorder, RoundedCornerShape(50)).pressClickable(onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun PadKey(label: String, modifier: Modifier, onClick: () -> Unit) =
    PadKey(modifier, KeyBg, onClick) { Text(label, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Ink) }

/** แป้นเครื่องคิดเลข: แถวบนมี = ⌫ และปุ่มบันทึกรายการ */
@Composable
fun CalcKeypad(onKey: (String) -> Unit, onSave: () -> Unit, onHide: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onHide).padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically) {
            LineIcon(Glyph.ChevronDown, InkSoft, Modifier.size(14.dp))
            Text(" ซ่อนแป้นพิมพ์", fontSize = 12.sp, color = InkSoft)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PadKey("=", Modifier.weight(1f)) { onKey("=") }
            PadKey("⌫", Modifier.weight(1f)) { onKey("⌫") }
            PadKey(Modifier.weight(2f), Moss, onSave) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LineIcon(Glyph.Wallet, Color.White, Modifier.size(22.dp))
                    Text("  บันทึกรายการ", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
            }
        }
        listOf(
            listOf("7", "8", "9", "+"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "×"),
            listOf(".", "0", "C", "÷")
        ).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { k -> PadKey(k, Modifier.weight(1f)) { onKey(k) } }
            }
        }
    }
}
