package com.patipan.finance.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.patipan.finance.FinanceVM

/** หน้าเข้าสู่ระบบ (บัญชีเดียวที่สร้างไว้ใน Supabase) แอปจำการเข้าสู่ระบบไว้ ไม่ต้องกรอกซ้ำ */
@Composable
fun LoginScreen(vm: FinanceVM) {
    val busy by vm.authBusy.collectAsState()
    val err by vm.authError.collectAsState()
    var email by rememberSaveable { mutableStateOf(vm.lastEmail) }
    var pw by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    val canGo = email.isNotBlank() && pw.isNotEmpty() && !busy

    Surface(Modifier.fillMaxSize(), color = Cream) {
        Column(Modifier.fillMaxSize().gridBackground().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("สมุดรายรับ-รายจ่าย", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Ink, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text("เข้าสู่ระบบเพื่อซิงก์ข้อมูลกับ Supabase", fontSize = 15.sp, color = InkSoft, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            SoftCard {
                OutlinedTextField(email, { email = it }, label = { Text("อีเมล") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(pw, { pw = it }, label = { Text("รหัสผ่าน") }, singleLine = true,
                    visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (canGo) vm.signIn(email, pw) }),
                    trailingIcon = { TextButton(onClick = { show = !show }) { Text(if (show) "ซ่อน" else "แสดง", color = Moss) } },
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
                val e = err
                if (e != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(e, color = ExpenseText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = { vm.signIn(email, pw) }, enabled = canGo,
                    modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Moss, contentColor = Color.White)) {
                    if (busy) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("เข้าสู่ระบบ", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** หน้ารอโหลดข้อมูลหลังเข้าสู่ระบบ / เปิดแอป ถ้าโหลดไม่สำเร็จ (เช่นไม่มีเน็ต) จะแสดงปุ่มลองใหม่ */
@Composable
fun LoadingScreen(vm: FinanceVM) {
    val err by vm.loadError.collectAsState()
    Surface(Modifier.fillMaxSize(), color = Cream) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            val e = err
            if (e == null) {
                CircularProgressIndicator(color = Moss)
                Spacer(Modifier.height(16.dp))
                Text("กำลังโหลดข้อมูล…", fontSize = 16.sp, color = InkSoft)
            } else {
                Text("โหลดข้อมูลไม่สำเร็จ", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(Modifier.height(8.dp))
                Text(e, fontSize = 14.sp, color = ExpenseText, textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                Button(onClick = { vm.reload() }, shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Moss, contentColor = Color.White)) {
                    Text("ลองใหม่", fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = { vm.signOut() }) { Text("ออกจากระบบ", color = ExpenseText) }
            }
        }
    }
}
