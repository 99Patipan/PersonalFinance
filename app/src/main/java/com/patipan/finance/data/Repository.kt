package com.patipan.finance.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class Repository(private val db: AppDatabase, private val ctx: Context) {
    private val dao = db.dao()

    val wallets = dao.walletsFlow()
    val categories = dao.categoriesFlow()
    val txns = dao.txnsFlow()
    val budgets = dao.budgetsFlow()

    suspend fun saveTxn(t: Txn) {
        if (t.id == 0L) dao.insertTxn(t) else dao.updateTxn(t)
    }

    suspend fun deleteTxn(t: Txn) {
        dao.deleteTxn(t.id)
        t.receiptPath?.let { runCatching { File(it).delete() } }
    }

    suspend fun saveWallet(w: Wallet) { if (w.id == 0L) dao.insertWallet(w) else dao.updateWallet(w) }

    /** คืนค่า null ถ้าลบได้, หรือข้อความเหตุผลถ้าลบไม่ได้ */
    suspend fun deleteWallet(w: Wallet): String? = db.withTransaction {
        when {
            dao.walletsOnce().size <= 1 -> "ต้องมีกระเป๋าเงินอย่างน้อย 1 ใบ"
            dao.txnCountForWallet(w.id) > 0 -> "ลบไม่ได้ เพราะมีรายการที่ใช้กระเป๋านี้อยู่"
            else -> { dao.deleteWallet(w.id); null }
        }
    }

    suspend fun saveCategory(c: Category) { if (c.id == 0L) dao.insertCategory(c) else dao.updateCategory(c) }

    suspend fun deleteCategory(c: Category) = db.withTransaction {
        dao.clearCategoryOnTxns(c.id)
        dao.deleteBudget(c.id)
        dao.deleteCategory(c.id)
    }

    suspend fun setBudget(categoryId: Long, cents: Long) {
        if (cents <= 0) dao.deleteBudget(categoryId) else dao.upsertBudget(Budget(categoryId, cents))
    }

    fun saveReceipt(uri: Uri): String? = runCatching {
        val dir = File(ctx.filesDir, "receipts").apply { mkdirs() }
        val f = File(dir, "rcpt_${System.currentTimeMillis()}.jpg")
        ctx.contentResolver.openInputStream(uri)!!.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
        f.absolutePath
    }.getOrNull()

    // ---------- CSV ----------
    suspend fun writeCsv(out: OutputStream) {
        val cats = dao.categoriesOnce().associateBy { it.id }
        val ws = dao.walletsOnce().associateBy { it.id }
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        fun esc(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
        fun amt(c: Long) = "${c / 100}.${(c % 100).toString().padStart(2, '0')}"
        val sb = StringBuilder("\uFEFF")
        sb.append("วันที่เวลา,ประเภท,หมวดหมู่,กระเป๋า,โอนไปยัง,จำนวนเงิน,รายละเอียด,หมายเหตุ\n")
        for (t in dao.txnsOnce()) {
            val type = when (t.type) { INCOME -> "รายรับ"; EXPENSE -> "รายจ่าย"; else -> "โอนเงิน" }
            val time = Instant.ofEpochMilli(t.timestamp).atZone(ZoneId.systemDefault()).format(fmt)
            sb.append(listOf(
                time, type, cats[t.categoryId]?.name ?: "", ws[t.walletId]?.name ?: "",
                ws[t.toWalletId]?.name ?: "", amt(t.amountCents), esc(t.detail), esc(t.note)
            ).mapIndexed { i, s -> if (i in 6..7) s else esc(s) }.joinToString(",")).append("\n")
        }
        out.write(sb.toString().toByteArray(Charsets.UTF_8))
    }

    // ---------- JSON backup ----------
    suspend fun writeBackup(out: OutputStream) {
        val root = JSONObject()
        root.put("app", "PersonalFinance")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("wallets", JSONArray().also { a ->
            dao.walletsOnce().forEach {
                a.put(JSONObject().put("id", it.id).put("name", it.name).put("color", it.color)
                    .put("initialCents", it.initialCents))
            }
        })
        root.put("categories", JSONArray().also { a ->
            dao.categoriesOnce().forEach {
                a.put(JSONObject().put("id", it.id).put("name", it.name).put("type", it.type).put("emoji", it.emoji))
            }
        })
        root.put("budgets", JSONArray().also { a ->
            dao.budgetsOnce().forEach {
                a.put(JSONObject().put("categoryId", it.categoryId).put("limitCents", it.limitCents))
            }
        })
        root.put("transactions", JSONArray().also { a ->
            dao.txnsOnce().forEach {
                val o = JSONObject().put("id", it.id).put("type", it.type).put("amountCents", it.amountCents)
                    .put("categoryId", it.categoryId ?: JSONObject.NULL)
                    .put("walletId", it.walletId)
                    .put("toWalletId", it.toWalletId ?: JSONObject.NULL)
                    .put("timestamp", it.timestamp).put("detail", it.detail).put("note", it.note)
                val f = it.receiptPath?.let { p -> File(p) }
                if (f != null && f.exists()) {
                    o.put("receiptB64", Base64.encodeToString(f.readBytes(), Base64.NO_WRAP))
                }
                a.put(o)
            }
        })
        out.write(root.toString().toByteArray(Charsets.UTF_8))
    }

    /** แทนที่ข้อมูลทั้งหมดด้วยไฟล์สำรอง (ทำใน Transaction เดียว) */
    suspend fun restoreBackup(input: InputStream) {
        val root = JSONObject(input.readBytes().toString(Charsets.UTF_8))
        require(root.optString("app") == "PersonalFinance") { "ไม่ใช่ไฟล์สำรองของแอปนี้" }
        val dir = File(ctx.filesDir, "receipts").apply { mkdirs() }

        val wallets = root.getJSONArray("wallets").let { a ->
            (0 until a.length()).map { a.getJSONObject(it) }.map {
                Wallet(it.getLong("id"), it.getString("name"), it.getLong("color"), it.getLong("initialCents"))
            }
        }
        val cats = root.getJSONArray("categories").let { a ->
            (0 until a.length()).map { a.getJSONObject(it) }.map {
                Category(it.getLong("id"), it.getString("name"), it.getString("type"), it.getString("emoji"))
            }
        }
        val budgets = root.getJSONArray("budgets").let { a ->
            (0 until a.length()).map { a.getJSONObject(it) }.map {
                Budget(it.getLong("categoryId"), it.getLong("limitCents"))
            }
        }
        val txns = root.getJSONArray("transactions").let { a ->
            (0 until a.length()).map { a.getJSONObject(it) }.map {
                var path: String? = null
                if (it.has("receiptB64")) {
                    val f = File(dir, "rcpt_restored_${it.getLong("id")}_${System.nanoTime()}.jpg")
                    f.writeBytes(Base64.decode(it.getString("receiptB64"), Base64.NO_WRAP))
                    path = f.absolutePath
                }
                Txn(
                    id = it.getLong("id"), type = it.getString("type"), amountCents = it.getLong("amountCents"),
                    categoryId = if (it.isNull("categoryId")) null else it.getLong("categoryId"),
                    walletId = it.getLong("walletId"),
                    toWalletId = if (it.isNull("toWalletId")) null else it.getLong("toWalletId"),
                    timestamp = it.getLong("timestamp"), detail = it.optString("detail", ""),
                    note = it.optString("note", ""), receiptPath = path
                )
            }
        }
        require(wallets.isNotEmpty()) { "ไฟล์สำรองไม่มีกระเป๋าเงิน" }
        db.withTransaction {
            dao.clearTxns(); dao.clearBudgets(); dao.clearCategories(); dao.clearWallets()
            dao.insertWallets(wallets); dao.insertCategories(cats)
            dao.insertBudgets(budgets); dao.insertTxns(txns)
        }
    }
}
