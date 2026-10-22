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

val Cream = Color(0xFFFFF7E0)
val Lime = Color(0xFFB9DB55)
val Yellow = Color(0xFFFFE28A)
val Pink = Color(0xFFFFC9D9)
val Mint = Color(0xFFCDEBC5)
val Orange = Color(0xFFFF8A4C)
val Ink = Color(0xFF4A3F35)
val IncomeGreen = Color(0xFF3E9B5A)
val ExpenseRed = Color(0xFFE5603A)

@Composable
fun FinanceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Orange, onPrimary = Color.White, secondary = Lime,
            background = Cream, surface = Color.White, onSurface = Ink, onBackground = Ink,
            surfaceVariant = Yellow.copy(alpha = 0.35f)
        ),
        content = content
    )
}

/** พื้นหลังครีมมีลายตารางบาง ๆ */
fun Modifier.gridBackground(): Modifier = this.background(Cream).drawBehind {
    val step = 26.dp.toPx()
    val c = Color(0x18C9A55A)
    var x = 0f
    while (x < size.width) { drawLine(c, Offset(x, 0f), Offset(x, size.height), 1f); x += step }
    var y = 0f
    while (y < size.height) { drawLine(c, Offset(0f, y), Offset(size.width, y), 1f); y += step }
}
