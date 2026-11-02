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

// ---- พาเลตต์ใหม่: ครีม + เขียวเสจ + ส้มพีช ----
val Cream = Color(0xFFFAF4E9)
val Sage = Color(0xFF8DAA93)
val SageDeep = Color(0xFF6F9479)
val SageLight = Color(0xFFB7CBBB)
val Lime = Color(0xFF8DAA93)          // (ชื่อเดิม) = เสจ
val Forest = Color(0xFF466F55)        // การ์ดยอดรวม (เข้มขึ้นให้ตัวอักษรขาวอ่านชัด)
val ForestLight = Color(0xFF5E8769)
val Yellow = Color(0xFFFBE6D8)        // (ชื่อเดิม) = พีชอ่อน ใช้เป็นสีวันที่เลือก
val Pink = Color(0xFFFBEAE4)
val Mint = Color(0xFFE4EFEA)
val Sand = Color(0xFFE9E8DE)          // พื้นช่อง/รางโปรเกรส
val Orange = Color(0xFFF59A62)
val OrangeLight = Color(0xFFF9C3A1)
val Ink = Color(0xFF3A403B)
val IncomeGreen = Color(0xFF6F9479)   // สีเติม (แท่งกราฟ จุด)
val ExpenseRed = Color(0xFFE9824B)    // สีเติม (แท่งกราฟ จุด)

// สีสำหรับ "ตัวอักษร/ไอคอน" ที่ต้องอ่านชัดบนพื้นขาวและครีม (คอนทราสต์ ≥ 4.5:1)
val Moss = Color(0xFF4A7C59)          // เขียวหลัก: ไอคอนที่เลือก ปุ่ม ลิงก์ รายรับ
val IncomeText = Moss
val ExpenseText = Color(0xFFB84E17)   // ส้มอิฐ: รายจ่าย คำเตือน ลบ
val Action = Color(0xFFE07A3F)        // ปุ่มหลัก "เพิ่มรายการ" (ไอคอนขาวบนสีนี้ ≥ 3:1)
val InkSoft = Ink.copy(alpha = 0.68f) // ข้อความรอง
val Num = TextStyle(fontFeatureSettings = "tnum") // ตัวเลขกว้างเท่ากัน ยอดเงินเรียงตรงหลัก

@Composable
fun FinanceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Moss, onPrimary = Color.White, secondary = Sage,
            outline = Color(0xFFB7BBB0), outlineVariant = Color(0xFFDADCD2),
            background = Cream, surface = Color.White, onSurface = Ink, onBackground = Ink,
            surfaceVariant = Sand,
            secondaryContainer = Mint, onSecondaryContainer = Ink,
            surfaceContainerHigh = Color(0xFFFFFCF7)
        ),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(14.dp), small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(26.dp), extraLarge = RoundedCornerShape(30.dp)
        ),
        content = content
    )
}

/** พื้นหลังครีมเรียบ ๆ (ตามดีไซน์ใหม่) */
fun Modifier.gridBackground(): Modifier = this.background(Cream)
