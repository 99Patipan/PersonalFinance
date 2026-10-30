package com.patipan.finance.data

import org.json.JSONObject

const val INCOME = "INCOME"
const val EXPENSE = "EXPENSE"
const val TRANSFER = "TRANSFER"

/** id เป็น UUID (String) สร้างฝั่งแอป; id ว่าง = ยังไม่เคยบันทึก */
data class Wallet(
    val id: String = "",
    val name: String,
    val color: Long,
    val initialCents: Long
)

data class Category(
    val id: String = "",
    val name: String,
    val type: String,
    /** อีโมจิ หรือ "icon:<กลุ่ม>/<ชื่อ>" สำหรับไอคอนรูปภาพ */
    val emoji: String,
    /** ลำดับที่แสดง (น้อยไปมาก) */
    val sortOrder: Int = 0
)

data class Txn(
    val id: String = "",
    val type: String,
    val amountCents: Long,
    val categoryId: String?,
    val walletId: String,
    val toWalletId: String?,
    val timestamp: Long,
    val detail: String,
    val note: String,
    /** path ของรูปใน Supabase Storage (bucket receipts) เช่น "<userId>/<uuid>.jpg" */
    val receiptPath: String?
)

data class Budget(
    val categoryId: String,
    val limitCents: Long
)

// ---------- แปลงไป-กลับ JSON ของตารางใน Supabase ----------

private fun JSONObject.strOrNull(k: String): String? = if (isNull(k)) null else getString(k)

fun Wallet.toJson(userId: String): JSONObject = JSONObject()
    .put("id", id).put("user_id", userId).put("name", name)
    .put("color", color).put("initial_cents", initialCents)

fun JSONObject.toWallet() = Wallet(getString("id"), getString("name"), getLong("color"), getLong("initial_cents"))

fun Category.toJson(userId: String): JSONObject = JSONObject()
    .put("id", id).put("user_id", userId).put("name", name).put("type", type).put("emoji", emoji).put("sort_order", sortOrder)

fun JSONObject.toCategory() = Category(getString("id"), getString("name"), getString("type"), optString("emoji", ""), optInt("sort_order", 0))

fun Budget.toJson(userId: String): JSONObject = JSONObject()
    .put("user_id", userId).put("category_id", categoryId).put("limit_cents", limitCents)

fun JSONObject.toBudget() = Budget(getString("category_id"), getLong("limit_cents"))

fun Txn.toJson(userId: String): JSONObject = JSONObject()
    .put("id", id).put("user_id", userId).put("type", type).put("amount_cents", amountCents)
    .put("category_id", categoryId ?: JSONObject.NULL)
    .put("wallet_id", walletId)
    .put("to_wallet_id", toWalletId ?: JSONObject.NULL)
    .put("timestamp_ms", timestamp).put("detail", detail).put("note", note)
    .put("receipt_path", receiptPath ?: JSONObject.NULL)

fun JSONObject.toTxn() = Txn(
    id = getString("id"), type = getString("type"), amountCents = getLong("amount_cents"),
    categoryId = strOrNull("category_id"), walletId = getString("wallet_id"),
    toWalletId = strOrNull("to_wallet_id"), timestamp = getLong("timestamp_ms"),
    detail = optString("detail", ""), note = optString("note", ""), receiptPath = strOrNull("receipt_path")
)
