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
private fun SettingRow(emoji: String, tint: Color, title: String, sub: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(vertical = 8.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(tint.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
            Text(emoji, fontSize = 23.sp)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(sub, fontSize = 12.5.sp, color = Ink.copy(alpha = 0.6f))
        }
        Box(Modifier.size(28.dp).clip(CircleShape).background(Sand), contentAlignment = Alignment.Center) {
            Text("›", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink.copy(alpha = 0.7f))
        }
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
        item {
            SoftCard {
                SectionTitle("จัดการข้อมูล")
                SettingRow("👛", Color(0xFFFFC857), "กระเป๋าเงิน", "เพิ่ม แก้ไข ลบ และดูยอดคงเหลือ") { onNav("wallets") }
                SettingRow("🏷️", Color(0xFFF58FB0), "หมวดหมู่", "จัดการหมวดรายรับ-รายจ่าย") { onNav("categories") }
                SettingRow("🎯", Color(0xFFFF8A4C), "งบประมาณรายเดือน", "ตั้งงบแยกตามหมวดหมู่") { onNav("budgets") }
            }
        }
        item {
            SoftCard {
                SectionTitle("🔔 แจ้งเตือนงบประมาณ")
                Text("เตือนเมื่อใช้เงินถึง $warn% ของงบ", fontSize = 14.sp, modifier = Modifier.padding(vertical = 6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(70, 80, 90).forEach { FilterChip(selected = warn == it, onClick = { vm.setWarn(it) }, label = { Text("$it%") }) }
                }
            }
        }
        item {
            SoftCard {
                SectionTitle("สำรองและส่งออกข้อมูล")
                SettingRow("📤", Color(0xFF6CB6E8), "ส่งออกเป็น CSV", "เปิดใน Excel / Google Sheets ได้") { csvL.launch("PersonalFinance_$stamp.csv") }
                SettingRow("💾", Color(0xFFB59AE8), "สำรองข้อมูลเป็น JSON", "รวมรูปใบเสร็จ เลือกตำแหน่งเก็บไฟล์เอง") { jsonL.launch("PersonalFinance_backup_$stamp.json") }
                SettingRow("📥", Color(0xFF7BC47F), "กู้คืนจากไฟล์สำรอง", "แทนที่ข้อมูลทั้งหมดในแอป") { openL.launch(arrayOf("application/json", "text/plain", "*/*")) }
            }
        }
        item {
            SoftCard(tint = Mint.copy(alpha = 0.5f)) {
                Text("🔒 ความเป็นส่วนตัว", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("ข้อมูลทั้งหมดอยู่ในเครื่องของคุณเท่านั้น แอปไม่ใช้อินเทอร์เน็ต ไม่มีโฆษณา ไม่มีการติดตามผู้ใช้ และไม่ต้องล็อกอิน",
                    fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
