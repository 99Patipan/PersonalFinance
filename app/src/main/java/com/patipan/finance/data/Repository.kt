package com.patipan.finance.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

private fun JSONArray.objs(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
private fun newId(): String = UUID.randomUUID().toString()

/**
 * ชั้นข้อมูล: เก็บสถานะล่าสุดจาก Supabase ไว้ใน StateFlow ให้ UI อ่าน (เหมือน Flow จาก Room เดิม)
 * ทุกการแก้ไขเขียนขึ้นเซิร์ฟเวอร์ก่อน สำเร็จแล้วจึงอัปเดตสถานะในเครื่อง ถ้าล้มเหลวจะ throw ให้ ViewModel แจ้งผู้ใช้
 */
class Repository(private val api: SupabaseApi, private val ctx: Context) {
    private val _wallets = MutableStateFlow<List<Wallet>>(emptyList())
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    private val _txns = MutableStateFlow<List<Txn>>(emptyList())
    private val _budgets = MutableStateFlow<List<Budget>>(emptyList())
    private val _loaded = MutableStateFlow(false)

    val wallets: StateFlow<List<Wallet>> = _wallets
    val categories: StateFlow<List<Category>> = _categories
    val txns: StateFlow<List<Txn>> = _txns
    val budgets: StateFlow<List<Budget>> = _budgets
    val loaded: StateFlow<Boolean> = _loaded

    private fun userId(): String = api.userId ?: throw ApiException("ยังไม่ได้เข้าสู่ระบบ", 401)

    private val txnOrder = compareByDescending<Txn> { it.timestamp }.thenByDescending { it.id }

    // ---------- โหลดข้อมูล (blocking: เรียกบน IO เท่านั้น) ----------
    private fun fetchWallets() = api.select("wallets", "order=created_at,id").objs().map { it.toWallet() }
    private fun fetchCategories() = api.select("categories", "order=sort_order,created_at,id").objs().map { it.toCategory() }
    private fun fetchTxns() = api.select("transactions", "order=timestamp_ms.desc,id.desc").objs().map { it.toTxn() }
    private fun fetchBudgets() = api.select("budgets", "order=category_id").objs().map { it.toBudget() }

    suspend fun refreshAll() {
        coroutineScope {
            val w = async(Dispatchers.IO) { fetchWallets() }
            val c = async(Dispatchers.IO) { fetchCategories() }
            val t = async(Dispatchers.IO) { fetchTxns() }
            val b = async(Dispatchers.IO) { fetchBudgets() }
            _wallets.value = w.await()
            _categories.value = c.await()
            _txns.value = t.await()
            _budgets.value = b.await()
            _loaded.value = true
        }
    }

    /** ล้างข้อมูลในเครื่องตอนออกจากระบบ */
    suspend fun clear() {
        _loaded.value = false
        _wallets.value = emptyList(); _categories.value = emptyList()
        _txns.value = emptyList(); _budgets.value = emptyList()
        withContext(Dispatchers.IO) { runCatching { receiptDir().deleteRecursively() } }
    }

    // ---------- รายการเงิน ----------
    suspend fun saveTxn(t: Txn) {
        withContext<Unit>(Dispatchers.IO) {
            val uid = userId()
            val id = t.id.ifEmpty { newId() }
            val old = _txns.value.firstOrNull { it.id == id }
            val rows = api.upsert("transactions", JSONArray().put(t.copy(id = id).toJson(uid)))
            val row = rows.getJSONObject(0).toTxn()
            _txns.value = (_txns.value.filter { it.id != row.id } + row).sortedWith(txnOrder)
            val oldPath = old?.receiptPath
            if (oldPath != null && oldPath != row.receiptPath) runCatching { api.deleteObject(oldPath) }
        }
    }

    suspend fun deleteTxn(t: Txn) {
        withContext<Unit>(Dispatchers.IO) {
            api.delete("transactions", "id=eq.${t.id}")
            _txns.value = _txns.value.filter { it.id != t.id }
            val p = t.receiptPath
            if (p != null) runCatching { api.deleteObject(p) }
        }
    }

    // ---------- กระเป๋า ----------
    suspend fun saveWallet(w: Wallet) {
        withContext<Unit>(Dispatchers.IO) {
            val saved = w.copy(id = w.id.ifEmpty { newId() })
            api.upsert("wallets", JSONArray().put(saved.toJson(userId())), returning = false)
            _wallets.value = fetchWallets()
        }
    }

    /** คืนค่า null ถ้าลบได้, หรือข้อความเหตุผลถ้าลบไม่ได้ */
    suspend fun deleteWallet(w: Wallet): String? = withContext(Dispatchers.IO) {
        when {
            _wallets.value.size <= 1 -> "ต้องมีกระเป๋าเงินอย่างน้อย 1 ใบ"
            _txns.value.any { it.walletId == w.id || it.toWalletId == w.id } -> "ลบไม่ได้ เพราะมีรายการที่ใช้กระเป๋านี้อยู่"
            else -> {
                api.delete("wallets", "id=eq.${w.id}")
                _wallets.value = fetchWallets()
                null
            }
        }
    }

    // ---------- หมวดหมู่ ----------
    suspend fun saveCategory(c: Category) {
        withContext<Unit>(Dispatchers.IO) {
            val saved = if (c.id.isEmpty()) c.copy(id = newId(), sortOrder = nextCategoryOrder(c.type)) else c
            api.upsert("categories", JSONArray().put(saved.toJson(userId())), returning = false)
            _categories.value = fetchCategories()
        }
    }

    /** ลำดับของหมวดใหม่ = ต่อท้ายหมวดประเภทเดียวกัน */
    private fun nextCategoryOrder(type: String): Int =
        (_categories.value.filter { it.type == type }.maxOfOrNull { it.sortOrder } ?: -1) + 1

    /** จัดลำดับหมวดหมู่ใหม่ตามรายการ id (เรียงจากบนลงล่าง) อัปเดตในเครื่องทันที แล้วบันทึกขึ้นเซิร์ฟเวอร์ */
    suspend fun reorderCategories(ids: List<String>) {
        withContext<Unit>(Dispatchers.IO) {
            val before = _categories.value
            val byId = before.associateBy { it.id }
            val changed = ids.mapIndexedNotNull { i, id -> byId[id]?.takeIf { it.sortOrder != i }?.copy(sortOrder = i) }
            if (changed.isEmpty()) return@withContext
            val changedMap = changed.associateBy { it.id }
            _categories.value = before.map { changedMap[it.id] ?: it }.sortedBy { it.sortOrder }
            try {
                val rows = JSONArray()
                changed.forEach { rows.put(it.toJson(userId())) }
                api.upsert("categories", rows, returning = false)
            } catch (e: Exception) {
                runCatching { _categories.value = fetchCategories() }
                throw e
            }
        }
    }

    /** พิมพ์ชื่อหมวดเอง: ถ้ามีชื่อซ้ำ (ประเภทเดียวกัน) ใช้อันเดิม ไม่เช่นนั้นสร้างใหม่ */
    suspend fun findOrCreateCategory(name: String, type: String, emoji: String): String = withContext(Dispatchers.IO) {
        val n = name.trim()
        val hit = _categories.value.firstOrNull { it.type == type && it.name.equals(n, ignoreCase = true) }
        if (hit != null) {
            hit.id
        } else {
            val c = Category(newId(), n, type, emoji, nextCategoryOrder(type))
            api.upsert("categories", JSONArray().put(c.toJson(userId())), returning = false)
            _categories.value = fetchCategories()
            c.id
        }
    }

    /** ฐานข้อมูลจัดการต่อให้: รายการในหมวดนี้ category_id เป็น null, งบของหมวดนี้ถูกลบ (ตั้งไว้ใน foreign key) */
    suspend fun deleteCategory(c: Category) {
        withContext<Unit>(Dispatchers.IO) {
            api.delete("categories", "id=eq.${c.id}")
            _txns.value = _txns.value.map { if (it.categoryId == c.id) it.copy(categoryId = null) else it }
            _budgets.value = _budgets.value.filter { it.categoryId != c.id }
            _categories.value = _categories.value.filter { it.id != c.id }
        }
    }

    // ---------- งบประมาณ ----------
    suspend fun setBudget(categoryId: String, cents: Long) {
        withContext<Unit>(Dispatchers.IO) {
            if (cents <= 0) {
                api.delete("budgets", "category_id=eq.$categoryId")
            } else {
                api.upsert("budgets", JSONArray().put(Budget(categoryId, cents).toJson(userId())),
                    onConflict = "user_id,category_id", returning = false)
            }
            _budgets.value = fetchBudgets()
        }
    }

    // ---------- รูปใบเสร็จ (Supabase Storage + แคชในเครื่อง) ----------
    private fun receiptDir() = File(ctx.cacheDir, "receipts")
    private fun cacheFile(path: String) = File(receiptDir(), path.replace('/', '_'))

    /** ย่อรูป (ด้านยาวสุด 1280px, JPEG 72%) แล้วอัปโหลด คืน path ใน Storage หรือ null ถ้าไม่สำเร็จ */
    suspend fun saveReceipt(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = compressImage(uri)
            val path = "${userId()}/${newId()}.jpg"
            api.uploadObject(path, bytes)
            val f = cacheFile(path)
            f.parentFile?.mkdirs()
            f.writeBytes(bytes)
            path
        }.getOrNull()
    }

    suspend fun loadReceipt(path: String): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val b = receiptBytes(path)
            if (b == null) null else BitmapFactory.decodeByteArray(b, 0, b.size)
        }.getOrNull()
    }

    private fun receiptBytes(path: String): ByteArray? {
        val f = cacheFile(path)
        if (f.exists()) return f.readBytes()
        val b = api.downloadObject(path) ?: return null
        f.parentFile?.mkdirs()
        f.writeBytes(b)
        return b
    }

    private fun compressImage(uri: Uri): ByteArray {
        val resolver = ctx.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
        while (maxSide / sample > 2560) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        var bmp: Bitmap = resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: throw IllegalStateException("อ่านรูปไม่ได้")
        val rotation: Float = runCatching {
            resolver.openInputStream(uri)!!.use { s ->
                when (ExifInterface(s).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            }
        }.getOrDefault(0f)
        val longest = maxOf(bmp.width, bmp.height)
        val scale = if (longest > 1280) 1280f / longest else 1f
        if (scale < 1f || rotation != 0f) {
            val m = Matrix()
            m.postScale(scale, scale)
            m.postRotate(rotation)
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        }
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 72, out)
        return out.toByteArray()
    }

    // ---------- CSV ----------
    suspend fun writeCsv(out: OutputStream) {
        val cats = _categories.value.associateBy { it.id }
        val ws = _wallets.value.associateBy { it.id }
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        fun esc(s: String) = "\"" + s.replace("\"", "\"\"") + "\""
        fun amt(c: Long) = "${c / 100}.${(c % 100).toString().padStart(2, '0')}"
        val sb = StringBuilder("\uFEFF")
        sb.append("วันที่เวลา,ประเภท,หมวดหมู่,กระเป๋า,โอนไปยัง,จำนวนเงิน,รายละเอียด,หมายเหตุ\n")
        for (t in _txns.value.sortedBy { it.timestamp }) {
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
        withContext<Unit>(Dispatchers.IO) {
            val root = JSONObject()
            root.put("app", "PersonalFinance")
            root.put("version", 2)
            root.put("exportedAt", System.currentTimeMillis())
            root.put("wallets", JSONArray().also { a ->
                _wallets.value.forEach {
                    a.put(JSONObject().put("id", it.id).put("name", it.name).put("color", it.color)
                        .put("initialCents", it.initialCents).put("includeInTotal", it.includeInTotal))
                }
            })
            root.put("categories", JSONArray().also { a ->
                _categories.value.forEach {
                    a.put(JSONObject().put("id", it.id).put("name", it.name).put("type", it.type).put("emoji", it.emoji).put("sortOrder", it.sortOrder))
                }
            })
            root.put("budgets", JSONArray().also { a ->
                _budgets.value.forEach {
                    a.put(JSONObject().put("categoryId", it.categoryId).put("limitCents", it.limitCents))
                }
            })
            root.put("transactions", JSONArray().also { a ->
                _txns.value.sortedBy { it.timestamp }.forEach {
                    val o = JSONObject().put("id", it.id).put("type", it.type).put("amountCents", it.amountCents)
                        .put("categoryId", it.categoryId ?: JSONObject.NULL)
                        .put("walletId", it.walletId)
                        .put("toWalletId", it.toWalletId ?: JSONObject.NULL)
                        .put("timestamp", it.timestamp).put("detail", it.detail).put("note", it.note)
                    val p = it.receiptPath
                    if (p != null) {
                        val bytes = runCatching { receiptBytes(p) }.getOrNull()
                        if (bytes != null) o.put("receiptB64", Base64.encodeToString(bytes, Base64.NO_WRAP))
                    }
                    a.put(o)
                }
            })
            out.write(root.toString().toByteArray(Charsets.UTF_8))
        }
    }

    private fun upsertChunks(table: String, rows: List<JSONObject>, onConflict: String = "id") {
        rows.chunked(100).forEach { chunk ->
            val arr = JSONArray()
            chunk.forEach { arr.put(it) }
            api.upsert(table, arr, onConflict, returning = false)
        }
    }

    private fun deleteIds(table: String, ids: List<String>) {
        ids.chunked(40).forEach { api.delete(table, "id=in.(${it.joinToString(",")})") }
    }

    /**
     * แทนที่ข้อมูลทั้งหมดด้วยไฟล์สำรอง (รับได้ทั้งไฟล์สำรองจากแอปรุ่นออฟไลน์เดิมที่ id เป็นตัวเลข และไฟล์ใหม่)
     * ลำดับ: เพิ่มข้อมูลใหม่ทั้งหมดก่อน -> สำเร็จแล้วจึงลบของเก่า ถ้าพังกลางทางจะถอนของใหม่ออก ข้อมูลเดิมไม่หาย
     */
    suspend fun restoreBackup(input: InputStream) {
        withContext<Unit>(Dispatchers.IO) {
            val uid = userId()
            val root = JSONObject(input.readBytes().toString(Charsets.UTF_8))
            require(root.optString("app") == "PersonalFinance") { "ไม่ใช่ไฟล์สำรองของแอปนี้" }

            val wIds = HashMap<String, String>()
            val cIds = HashMap<String, String>()
            val wallets = root.getJSONArray("wallets").objs().map { o ->
                val nid = newId(); wIds[o.get("id").toString()] = nid
                Wallet(nid, o.getString("name"), o.getLong("color"), o.getLong("initialCents"), o.optBoolean("includeInTotal", true))
            }
            require(wallets.isNotEmpty()) { "ไฟล์สำรองไม่มีกระเป๋าเงิน" }
            val cats = root.getJSONArray("categories").objs().mapIndexed { idx, o ->
                val nid = newId(); cIds[o.get("id").toString()] = nid
                Category(nid, o.getString("name"), o.getString("type"), o.optString("emoji", ""), idx)
            }
            val budgets = root.getJSONArray("budgets").objs().mapNotNull { o ->
                val cid = cIds[o.get("categoryId").toString()]
                if (cid == null) null else Budget(cid, o.getLong("limitCents"))
            }

            val uploaded = mutableListOf<String>()
            val oldTxns = _txns.value
            val oldCats = _categories.value
            val oldWallets = _wallets.value
            var newTxnIds = emptyList<String>()
            try {
                val txns = root.getJSONArray("transactions").objs().map { o ->
                    var path: String? = null
                    if (o.has("receiptB64") && !o.isNull("receiptB64")) {
                        val bytes = Base64.decode(o.getString("receiptB64"), Base64.NO_WRAP)
                        val p = "$uid/${newId()}.jpg"
                        api.uploadObject(p, bytes)
                        uploaded.add(p)
                        path = p
                    }
                    val wid = wIds[o.get("walletId").toString()]
                        ?: throw IllegalArgumentException("ไฟล์สำรองไม่ถูกต้อง: รายการอ้างถึงกระเป๋าที่ไม่มี")
                    Txn(
                        id = newId(), type = o.getString("type"), amountCents = o.getLong("amountCents"),
                        categoryId = if (o.isNull("categoryId")) null else cIds[o.get("categoryId").toString()],
                        walletId = wid,
                        toWalletId = if (o.isNull("toWalletId")) null else wIds[o.get("toWalletId").toString()],
                        timestamp = o.getLong("timestamp"), detail = o.optString("detail", ""),
                        note = o.optString("note", ""), receiptPath = path
                    )
                }
                newTxnIds = txns.map { it.id }
                upsertChunks("wallets", wallets.map { it.toJson(uid) })
                upsertChunks("categories", cats.map { it.toJson(uid) })
                upsertChunks("budgets", budgets.map { it.toJson(uid) }, "user_id,category_id")
                upsertChunks("transactions", txns.map { it.toJson(uid) })
            } catch (e: Exception) {
                // ถอนข้อมูลใหม่ที่เพิ่มไปแล้วออก (ของเดิมยังอยู่ครบ)
                runCatching { deleteIds("transactions", newTxnIds) }
                runCatching { deleteIds("categories", cats.map { it.id }) }
                runCatching { deleteIds("wallets", wallets.map { it.id }) }
                uploaded.forEach { p -> runCatching { api.deleteObject(p) } }
                throw e
            }

            // เพิ่มใหม่สำเร็จครบแล้ว จึงลบของเก่า (รายการก่อน เพราะอ้างอิงกระเป๋าอยู่)
            deleteIds("transactions", oldTxns.map { it.id })
            deleteIds("categories", oldCats.map { it.id })
            deleteIds("wallets", oldWallets.map { it.id })
            oldTxns.mapNotNull { it.receiptPath }.forEach { p -> runCatching { api.deleteObject(p) } }
            runCatching { receiptDir().deleteRecursively() }
            refreshAll()
        }
    }
}
