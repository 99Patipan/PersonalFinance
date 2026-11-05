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

// ---- พาเลตต์ใหม่: ม่วงอินดิโก + ฟ้าลาเวนเดอร์ การ์ดโปร่งนุ่ม ----
// (ชื่อตัวแปรเดิมยังอยู่เพื่อไม่ต้องแก้ทุกหน้า แต่ค่าสีเปลี่ยนเป็นชุดใหม่ทั้งหมด)
val BgTop = Color(0xFFE8E3FF)         // พื้นหลังไล่สี: ม่วงลาเวนเดอร์ (ด้านบน)
val BgMid = Color(0xFFD8DCFF)
val BgBottom = Color(0xFFC6DDFA)      // ฟ้าอ่อน (ด้านล่าง)
val Cream = Color(0xFFF6F4FF)         // พื้นช่องกรอก/ตัวเลือก (ขาวอมม่วง)
val Violet = Color(0xFF6C5CF0)        // สีหลักของแบรนด์ (ปุ่ม + เมนู)
val VioletLight = Color(0xFF8B8CFF)
val Sage = Color(0xFF8B7FF7)          // (ชื่อเดิม) แถบงบประมาณปกติ = ม่วงอ่อน
val SageDeep = Color(0xFF6C5CF0)
val SageLight = Color(0xFFCFC9FB)
val Lime = Color(0xFF8B7FF7)
val Forest = Color(0xFF6656EE)        // การ์ดยอดรวม (ไล่สีม่วง ตัวอักษรขาวอ่านชัด)
val ForestLight = Color(0xFF7F8CFF)
val Yellow = Color(0xFFFFE9DC)        // (ชื่อเดิม) = พีชอ่อน วงกลมไอคอนรายจ่าย
val Pink = Color(0xFFFFE8E6)
val Mint = Color(0xFFE9E6FF)          // (ชื่อเดิม) = ลาเวนเดอร์อ่อน พื้นที่เลือก/ไอคอน
val MintGreen = Color(0xFFDDF5EA)     // เขียวอ่อน วงกลมไอคอนรายรับ
val Sand = Color(0xFFEEEBFB)          // พื้นช่อง/รางโปรเกรส
val Orange = Color(0xFFF5A06A)
val OrangeLight = Color(0xFFF9C9A8)
val Ink = Color(0xFF1F1B4D)           // กรมท่าเข้ม
val IncomeGreen = Color(0xFF3FB886)   // สีเติม (แท่งกราฟ จุด)
val ExpenseRed = Color(0xFFF2784B)    // สีเติม (แท่งกราฟ จุด)

// สีสำหรับ "ตัวอักษร/ไอคอน" ที่ต้องอ่านชัดบนพื้นขาว (คอนทราสต์ ≥ 4.5:1)
val Moss = Color(0xFF5B4BE0)          // ม่วงหลัก: ไอคอนที่เลือก ปุ่ม ลิงก์
val IncomeText = Color(0xFF16805A)    // เขียว: รายรับ
val ExpenseText = Color(0xFFC2410C)   // ส้มอิฐ: รายจ่าย คำเตือน ลบ
val Action = Violet                   // ปุ่มหลัก "เพิ่มรายการ"
val InkSoft = Ink.copy(alpha = 0.66f) // ข้อความรอง
val Num = TextStyle(fontFeatureSettings = "tnum") // ตัวเลขกว้างเท่ากัน ยอดเงินเรียงตรงหลัก

/** เงาม่วงนุ่ม ๆ ใช้กับการ์ดและแถบเมนู */
fun Modifier.softShadow(radius: Dp = 24.dp, elevation: Dp = 10.dp, alpha: Float = 0.16f): Modifier =
    this.shadow(elevation, RoundedCornerShape(radius), ambientColor = Violet.copy(alpha = alpha), spotColor = Violet.copy(alpha = alpha + 0.04f))

@Composable
fun FinanceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Moss, onPrimary = Color.White, secondary = Sage,
            outline = Color(0xFFB9B6D6), outlineVariant = Color(0xFFDDDAF0),
            background = BgTop, surface = Color.White, onSurface = Ink, onBackground = Ink,
            surfaceVariant = Sand,
            secondaryContainer = Mint, onSecondaryContainer = Ink,
            surfaceContainerHigh = Color(0xFFFBFAFF)
        ),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(14.dp), small = RoundedCornerShape(14.dp), medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(28.dp), extraLarge = RoundedCornerShape(32.dp)
        ),
        content = content
    )
}

/** พื้นหลังไล่สีม่วงลาเวนเดอร์ → ฟ้าอ่อน (ตามดีไซน์ใหม่) */
fun Modifier.gridBackground(): Modifier =
    this.background(Brush.verticalGradient(listOf(BgTop, BgMid, BgBottom)))
