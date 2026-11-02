package com.patipan.finance.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/** ไอคอนเส้นบาง ๆ วาดเองด้วย Canvas (ไม่ต้องพึ่งไลบรารีไอคอน) */
enum class Glyph {
    Home, List, Plus, Chart, Gear, Pencil,
    ChevronLeft, ChevronRight, ChevronDown, Close, Wallet, Tag, Target,
    Export, Save, Restore, Bell, Lock, Search, Calendar, Clock, Camera, Trash, Transfer
}

@Composable
fun LineIcon(glyph: Glyph, tint: Color, modifier: Modifier = Modifier.size(26.dp)) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.085f
        val st = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (glyph) {
            Glyph.Home -> {
                val roof = Path().apply {
                    moveTo(w * 0.10f, h * 0.48f); lineTo(w * 0.50f, h * 0.14f); lineTo(w * 0.90f, h * 0.48f)
                }
                val walls = Path().apply {
                    moveTo(w * 0.22f, h * 0.42f); lineTo(w * 0.22f, h * 0.86f)
                    lineTo(w * 0.78f, h * 0.86f); lineTo(w * 0.78f, h * 0.42f)
                }
                val door = Path().apply {
                    moveTo(w * 0.42f, h * 0.86f); lineTo(w * 0.42f, h * 0.62f)
                    lineTo(w * 0.58f, h * 0.62f); lineTo(w * 0.58f, h * 0.86f)
                }
                drawPath(roof, tint, style = st)
                drawPath(walls, tint, style = st)
                drawPath(door, tint, style = st)
            }
            Glyph.List -> {
                drawRoundRect(
                    color = tint, topLeft = Offset(w * 0.18f, h * 0.12f), size = Size(w * 0.64f, h * 0.76f),
                    cornerRadius = CornerRadius(w * 0.12f, w * 0.12f), style = st
                )
                for (i in 0..2) {
                    val y = h * (0.34f + i * 0.16f)
                    drawLine(color = tint, start = Offset(w * 0.33f, y), end = Offset(w * 0.67f, y), strokeWidth = sw, cap = StrokeCap.Round)
                }
            }
            Glyph.Plus -> {
                drawLine(color = tint, start = Offset(w * 0.50f, h * 0.18f), end = Offset(w * 0.50f, h * 0.82f), strokeWidth = sw, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.18f, h * 0.50f), end = Offset(w * 0.82f, h * 0.50f), strokeWidth = sw, cap = StrokeCap.Round)
            }
            Glyph.Chart -> {
                drawLine(color = tint, start = Offset(w * 0.26f, h * 0.84f), end = Offset(w * 0.26f, h * 0.52f), strokeWidth = sw * 1.5f, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.50f, h * 0.84f), end = Offset(w * 0.50f, h * 0.18f), strokeWidth = sw * 1.5f, cap = StrokeCap.Round)
                drawLine(color = tint, start = Offset(w * 0.74f, h * 0.84f), end = Offset(w * 0.74f, h * 0.38f), strokeWidth = sw * 1.5f, cap = StrokeCap.Round)
            }
            Glyph.Gear -> {
                val c = Offset(w * 0.5f, h * 0.5f)
                drawCircle(color = tint, radius = w * 0.27f, center = c, style = st)
                drawCircle(color = tint, radius = w * 0.10f, center = c, style = st)
                for (i in 0 until 8) {
                    val a = (i * 45f) * (3.14159265f / 180f)
                    val r1 = w * 0.30f
                    val r2 = w * 0.42f
                    drawLine(
                        color = tint,
                        start = Offset(c.x + r1 * cos(a), c.y + r1 * sin(a)),
                        end = Offset(c.x + r2 * cos(a), c.y + r2 * sin(a)),
                        strokeWidth = sw * 1.3f, cap = StrokeCap.Round
                    )
                }
            }
            Glyph.Pencil -> {
                val body = Path().apply {
                    moveTo(w * 0.16f, h * 0.84f); lineTo(w * 0.22f, h * 0.62f)
                    lineTo(w * 0.66f, h * 0.18f); lineTo(w * 0.82f, h * 0.34f)
                    lineTo(w * 0.38f, h * 0.78f); close()
                }
                drawPath(body, tint, style = st)
                drawLine(color = tint, start = Offset(w * 0.56f, h * 0.28f), end = Offset(w * 0.72f, h * 0.44f), strokeWidth = sw, cap = StrokeCap.Round)
            }
            Glyph.ChevronLeft -> drawPath(Path().apply {
                moveTo(w * 0.62f, h * 0.22f); lineTo(w * 0.34f, h * 0.50f); lineTo(w * 0.62f, h * 0.78f)
            }, tint, style = st)
            Glyph.ChevronRight -> drawPath(Path().apply {
                moveTo(w * 0.38f, h * 0.22f); lineTo(w * 0.66f, h * 0.50f); lineTo(w * 0.38f, h * 0.78f)
            }, tint, style = st)
            Glyph.ChevronDown -> drawPath(Path().apply {
                moveTo(w * 0.22f, h * 0.38f); lineTo(w * 0.50f, h * 0.66f); lineTo(w * 0.78f, h * 0.38f)
            }, tint, style = st)
            Glyph.Close -> {
                drawLine(tint, Offset(w * 0.24f, h * 0.24f), Offset(w * 0.76f, h * 0.76f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.76f, h * 0.24f), Offset(w * 0.24f, h * 0.76f), sw, StrokeCap.Round)
            }
            Glyph.Wallet -> {
                drawRoundRect(tint, Offset(w * 0.12f, h * 0.24f), Size(w * 0.76f, h * 0.56f), CornerRadius(w * 0.12f, w * 0.12f), style = st)
                drawLine(tint, Offset(w * 0.12f, h * 0.40f), Offset(w * 0.88f, h * 0.40f), sw, StrokeCap.Round)
                drawCircle(tint, radius = w * 0.045f, center = Offset(w * 0.72f, h * 0.60f))
            }
            Glyph.Tag -> {
                drawPath(Path().apply {
                    moveTo(w * 0.14f, h * 0.14f); lineTo(w * 0.52f, h * 0.14f); lineTo(w * 0.88f, h * 0.50f)
                    lineTo(w * 0.52f, h * 0.86f); lineTo(w * 0.14f, h * 0.48f); close()
                }, tint, style = st)
                drawCircle(tint, radius = w * 0.06f, center = Offset(w * 0.32f, h * 0.32f), style = st)
            }
            Glyph.Target -> {
                val c = Offset(w * 0.5f, h * 0.5f)
                drawCircle(tint, radius = w * 0.36f, center = c, style = st)
                drawCircle(tint, radius = w * 0.21f, center = c, style = st)
                drawCircle(tint, radius = w * 0.05f, center = c)
            }
            Glyph.Export -> {
                drawPath(Path().apply {
                    moveTo(w * 0.20f, h * 0.58f); lineTo(w * 0.20f, h * 0.82f); lineTo(w * 0.80f, h * 0.82f); lineTo(w * 0.80f, h * 0.58f)
                }, tint, style = st)
                drawLine(tint, Offset(w * 0.50f, h * 0.64f), Offset(w * 0.50f, h * 0.16f), sw, StrokeCap.Round)
                drawPath(Path().apply {
                    moveTo(w * 0.34f, h * 0.32f); lineTo(w * 0.50f, h * 0.16f); lineTo(w * 0.66f, h * 0.32f)
                }, tint, style = st)
            }
            Glyph.Restore -> {
                drawPath(Path().apply {
                    moveTo(w * 0.20f, h * 0.58f); lineTo(w * 0.20f, h * 0.82f); lineTo(w * 0.80f, h * 0.82f); lineTo(w * 0.80f, h * 0.58f)
                }, tint, style = st)
                drawLine(tint, Offset(w * 0.50f, h * 0.16f), Offset(w * 0.50f, h * 0.64f), sw, StrokeCap.Round)
                drawPath(Path().apply {
                    moveTo(w * 0.34f, h * 0.48f); lineTo(w * 0.50f, h * 0.64f); lineTo(w * 0.66f, h * 0.48f)
                }, tint, style = st)
            }
            Glyph.Save -> {
                drawRoundRect(tint, Offset(w * 0.16f, h * 0.16f), Size(w * 0.68f, h * 0.68f), CornerRadius(w * 0.10f, w * 0.10f), style = st)
                drawLine(tint, Offset(w * 0.34f, h * 0.16f), Offset(w * 0.34f, h * 0.36f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.34f, h * 0.36f), Offset(w * 0.64f, h * 0.36f), sw, StrokeCap.Round)
                drawRoundRect(tint, Offset(w * 0.32f, h * 0.56f), Size(w * 0.36f, h * 0.28f), CornerRadius(w * 0.04f, w * 0.04f), style = st)
            }
            Glyph.Bell -> {
                drawPath(Path().apply {
                    moveTo(w * 0.28f, h * 0.72f)
                    cubicTo(w * 0.28f, h * 0.55f, w * 0.30f, h * 0.28f, w * 0.50f, h * 0.24f)
                    cubicTo(w * 0.70f, h * 0.28f, w * 0.72f, h * 0.55f, w * 0.72f, h * 0.72f)
                }, tint, style = st)
                drawLine(tint, Offset(w * 0.20f, h * 0.72f), Offset(w * 0.80f, h * 0.72f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.43f, h * 0.85f), Offset(w * 0.57f, h * 0.85f), sw, StrokeCap.Round)
            }
            Glyph.Lock -> {
                drawRoundRect(tint, Offset(w * 0.20f, h * 0.44f), Size(w * 0.60f, h * 0.42f), CornerRadius(w * 0.08f, w * 0.08f), style = st)
                drawPath(Path().apply {
                    moveTo(w * 0.32f, h * 0.44f); lineTo(w * 0.32f, h * 0.32f)
                    cubicTo(w * 0.32f, h * 0.12f, w * 0.68f, h * 0.12f, w * 0.68f, h * 0.32f)
                    lineTo(w * 0.68f, h * 0.44f)
                }, tint, style = st)
            }
            Glyph.Search -> {
                drawCircle(tint, radius = w * 0.26f, center = Offset(w * 0.44f, h * 0.44f), style = st)
                drawLine(tint, Offset(w * 0.64f, h * 0.64f), Offset(w * 0.84f, h * 0.84f), sw, StrokeCap.Round)
            }
            Glyph.Calendar -> {
                drawRoundRect(tint, Offset(w * 0.16f, h * 0.22f), Size(w * 0.68f, h * 0.62f), CornerRadius(w * 0.10f, w * 0.10f), style = st)
                drawLine(tint, Offset(w * 0.16f, h * 0.42f), Offset(w * 0.84f, h * 0.42f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.34f, h * 0.12f), Offset(w * 0.34f, h * 0.28f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.66f, h * 0.12f), Offset(w * 0.66f, h * 0.28f), sw, StrokeCap.Round)
            }
            Glyph.Clock -> {
                drawCircle(tint, radius = w * 0.36f, center = Offset(w * 0.5f, h * 0.5f), style = st)
                drawPath(Path().apply {
                    moveTo(w * 0.50f, h * 0.30f); lineTo(w * 0.50f, h * 0.50f); lineTo(w * 0.64f, h * 0.58f)
                }, tint, style = st)
            }
            Glyph.Camera -> {
                drawRoundRect(tint, Offset(w * 0.12f, h * 0.28f), Size(w * 0.76f, h * 0.54f), CornerRadius(w * 0.10f, w * 0.10f), style = st)
                drawCircle(tint, radius = w * 0.14f, center = Offset(w * 0.5f, h * 0.55f), style = st)
                drawPath(Path().apply {
                    moveTo(w * 0.38f, h * 0.28f); lineTo(w * 0.44f, h * 0.18f); lineTo(w * 0.56f, h * 0.18f); lineTo(w * 0.62f, h * 0.28f)
                }, tint, style = st)
            }
            Glyph.Trash -> {
                drawLine(tint, Offset(w * 0.18f, h * 0.28f), Offset(w * 0.82f, h * 0.28f), sw, StrokeCap.Round)
                drawPath(Path().apply {
                    moveTo(w * 0.40f, h * 0.28f); lineTo(w * 0.40f, h * 0.16f); lineTo(w * 0.60f, h * 0.16f); lineTo(w * 0.60f, h * 0.28f)
                }, tint, style = st)
                drawPath(Path().apply {
                    moveTo(w * 0.28f, h * 0.28f); lineTo(w * 0.33f, h * 0.84f); lineTo(w * 0.67f, h * 0.84f); lineTo(w * 0.72f, h * 0.28f)
                }, tint, style = st)
                drawLine(tint, Offset(w * 0.44f, h * 0.44f), Offset(w * 0.44f, h * 0.70f), sw, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.56f, h * 0.44f), Offset(w * 0.56f, h * 0.70f), sw, StrokeCap.Round)
            }
            Glyph.Transfer -> {
                drawLine(tint, Offset(w * 0.18f, h * 0.34f), Offset(w * 0.82f, h * 0.34f), sw, StrokeCap.Round)
                drawPath(Path().apply {
                    moveTo(w * 0.66f, h * 0.18f); lineTo(w * 0.82f, h * 0.34f); lineTo(w * 0.66f, h * 0.50f)
                }, tint, style = st)
                drawLine(tint, Offset(w * 0.82f, h * 0.68f), Offset(w * 0.18f, h * 0.68f), sw, StrokeCap.Round)
                drawPath(Path().apply {
                    moveTo(w * 0.34f, h * 0.52f); lineTo(w * 0.18f, h * 0.68f); lineTo(w * 0.34f, h * 0.84f)
                }, tint, style = st)
            }
        }
    }
}
