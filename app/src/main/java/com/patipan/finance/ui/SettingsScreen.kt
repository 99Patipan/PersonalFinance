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
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
private fun SettingRow(glyph: Glyph, title: String, sub: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).pressClickable(onClick).padding(vertical = 8.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically) {
        // ไอคอนเส้นชุดเดียวกันทั้งแอป บนแผ่นสีมิ้นต์ (ไม่ใช้อีโมจิหลายสี)
        Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(Mint), contentAlignment = Alignment.Center) {
            LineIcon(glyph, Moss, Modifier.size(24.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(sub, fontSize = 13.sp, color = InkSoft)
        }
        LineIcon(Glyph.ChevronRight, InkSoft, Modifier.size(22.dp))
    }
}

@Composable
fun SettingsScreen(vm: FinanceVM, onNav: (String) -> Unit) {
    val warn by vm.warnPercent.collectAsState()
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    val stamp = LocalDate.now().toString()
    val csvL = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { u -> if (u != null) vm.exportCsv(u) }
    val jsonL = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { u -> if (u != null) vm.backup(u) }
    val openL = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u -> restoreUri = u }

    restoreUri?.let { u ->
        ConfirmDialog("กู้คืนข้อมูลจากไฟล์สำรอง?",
            "คำเตือน: ข้อมูลทั้งหมดที่มีอยู่ในแอปตอนนี้ (กระเป๋า หมวดหมู่ รายการ งบประมาณ) จะถูกแทนที่ด้วยข้อมูลในไฟล์ " +
                "และไม่สามารถย้อนกลับได้ แนะนำให้สำรองข้อมูลปัจจุบันก่อน",
            "กู้คืน", onConfirm = { vm.restore(u); restoreUri = null }, onDismiss = { restoreUri = null })
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ScreenTitle("ตั้งค่า") }
        item {
            SoftCard {
                SectionTitle("จัดการข้อมูล")
                SettingRow(Glyph.Wallet, "กระเป๋าเงิน", "เพิ่ม แก้ไข ลบ และดูยอดคงเหลือ") { onNav("wallets") }
                SettingRow(Glyph.Tag, "หมวดหมู่", "จัดการหมวดรายรับ-รายจ่าย") { onNav("categories") }
                SettingRow(Glyph.Target, "งบประมาณรายเดือน", "ตั้งงบแยกตามหมวดหมู่") { onNav("budgets") }
            }
        }
        item {
            SoftCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LineIcon(Glyph.Bell, Moss, Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    SectionTitle("แจ้งเตือนงบประมาณ")
                }
                Text("เตือนเมื่อใช้เงินถึง $warn% ของงบ", fontSize = 15.sp, color = InkSoft, modifier = Modifier.padding(vertical = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(70, 80, 90).forEach { Pill("$it%", warn == it) { vm.setWarn(it) } }
                }
            }
        }
        item {
            SoftCard {
                SectionTitle("สำรองและส่งออกข้อมูล")
                SettingRow(Glyph.Export, "ส่งออกเป็น CSV", "เปิดใน Excel / Google Sheets ได้") { csvL.launch("PersonalFinance_$stamp.csv") }
                SettingRow(Glyph.Save, "สำรองข้อมูลเป็น JSON", "รวมรูปใบเสร็จ เลือกตำแหน่งเก็บไฟล์เอง") { jsonL.launch("PersonalFinance_backup_$stamp.json") }
                SettingRow(Glyph.Restore, "กู้คืนจากไฟล์สำรอง", "แทนที่ข้อมูลทั้งหมดในแอป") { openL.launch(arrayOf("application/json", "text/plain", "*/*")) }
            }
        }
        item {
            SoftCard(tint = Mint.copy(alpha = 0.5f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LineIcon(Glyph.Lock, Moss, Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("ความเป็นส่วนตัว", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
                Text("ข้อมูลทั้งหมดอยู่ในเครื่องของคุณเท่านั้น แอปไม่ใช้อินเทอร์เน็ต ไม่มีโฆษณา ไม่มีการติดตามผู้ใช้ และไม่ต้องล็อกอิน",
                    fontSize = 14.sp, color = InkSoft, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}
