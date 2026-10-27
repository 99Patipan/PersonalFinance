package com.patipan.finance.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

class ApiException(message: String, val code: Int = 0) : Exception(message)

class HttpResult(val code: Int, val body: ByteArray) {
    val ok: Boolean get() = code in 200..299
    val text: String get() = body.toString(Charsets.UTF_8)
}

/** แปลงข้อผิดพลาดเป็นข้อความภาษาไทยที่แสดงให้ผู้ใช้ได้ */
fun friendlyError(e: Throwable): String = when (e) {
    is ApiException -> e.message ?: "เกิดข้อผิดพลาด"
    is UnknownHostException, is SocketTimeoutException, is ConnectException ->
        "เชื่อมต่ออินเทอร์เน็ตไม่ได้ กรุณาตรวจสอบสัญญาณแล้วลองใหม่"
    is IOException -> "การเชื่อมต่อมีปัญหา กรุณาลองใหม่"
    else -> e.message ?: "เกิดข้อผิดพลาด"
}

/**
 * ตัวเชื่อมต่อ Supabase แบบ REST ล้วน (Auth / PostgREST / Storage) ใช้ HttpURLConnection + org.json
 * ทุกเมธอดเป็น blocking ต้องเรียกบน Dispatchers.IO
 */
class SupabaseApi(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("sb_session", Context.MODE_PRIVATE)
    private val lock = Any()

    private val _signedIn = MutableStateFlow(sp.getString("refresh", null) != null)
    val signedIn: StateFlow<Boolean> = _signedIn

    val userId: String? get() = sp.getString("uid", null)
    val email: String? get() = sp.getString("email", null)
    val lastEmail: String? get() = sp.getString("last_email", null)

    // ---------------- Auth ----------------
    fun signIn(email: String, password: String) {
        val mail = email.trim()
        val body = JSONObject().put("email", mail).put("password", password).toString().toByteArray(Charsets.UTF_8)
        val r = raw("POST", "${SupabaseConfig.URL}/auth/v1/token?grant_type=password", body, "application/json", emptyMap(), null)
        if (!r.ok) throw ApiException(authMessage(r), r.code)
        saveSession(JSONObject(r.text), mail)
    }

    fun signOut() = clearSession()

    private fun saveSession(j: JSONObject, mail: String?) {
        val exp = System.currentTimeMillis() + j.optLong("expires_in", 3600L) * 1000L
        val uid = j.optJSONObject("user")?.optString("id")
        val e = sp.edit()
        e.putString("access", j.getString("access_token"))
        e.putString("refresh", j.getString("refresh_token"))
        e.putLong("exp", exp)
        if (!uid.isNullOrEmpty()) e.putString("uid", uid)
        if (mail != null) { e.putString("email", mail); e.putString("last_email", mail) }
        e.apply()
        _signedIn.value = true
    }

    private fun clearSession() {
        val e = sp.edit()
        e.remove("access"); e.remove("refresh"); e.remove("exp"); e.remove("uid"); e.remove("email")
        e.apply()
        _signedIn.value = false
    }

    private fun refreshSession() {
        val rt = sp.getString("refresh", null)
        if (rt == null) { clearSession(); throw ApiException("หมดอายุการเข้าสู่ระบบ กรุณาเข้าสู่ระบบใหม่", 401) }
        val body = JSONObject().put("refresh_token", rt).toString().toByteArray(Charsets.UTF_8)
        val r = raw("POST", "${SupabaseConfig.URL}/auth/v1/token?grant_type=refresh_token", body, "application/json", emptyMap(), null)
        if (r.ok) { saveSession(JSONObject(r.text), null); return }
        if (r.code in 400..499) { clearSession(); throw ApiException("หมดอายุการเข้าสู่ระบบ กรุณาเข้าสู่ระบบใหม่", 401) }
        throw ApiException(authMessage(r), r.code)
    }

    private fun token(): String = synchronized(lock) {
        val exp = sp.getLong("exp", 0L)
        if (sp.getString("access", null) == null || System.currentTimeMillis() > exp - 60_000L) refreshSession()
        sp.getString("access", null) ?: throw ApiException("ยังไม่ได้เข้าสู่ระบบ", 401)
    }

    // ---------------- HTTP ----------------
    private fun raw(method: String, url: String, body: ByteArray?, contentType: String?,
                    headers: Map<String, String>, bearer: String?): HttpResult {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 15_000
            c.readTimeout = 60_000
            c.requestMethod = method
            c.setRequestProperty("apikey", SupabaseConfig.API_KEY)
            if (bearer != null) c.setRequestProperty("Authorization", "Bearer $bearer")
            if (contentType != null) c.setRequestProperty("Content-Type", contentType)
            for ((k, v) in headers) c.setRequestProperty(k, v)
            if (body != null) {
                c.doOutput = true
                c.setFixedLengthStreamingMode(body.size)
                c.outputStream.use { it.write(body) }
            }
            val code = c.responseCode
            val stream = if (code >= 400) c.errorStream else c.inputStream
            val bytes = stream?.use { it.readBytes() } ?: ByteArray(0)
            return HttpResult(code, bytes)
        } finally {
            c.disconnect()
        }
    }

    /** เรียกพร้อม token ของผู้ใช้ ถ้าโดน 401 จะรีเฟรช token แล้วลองอีกครั้ง */
    private fun call(method: String, url: String, body: ByteArray? = null, contentType: String? = null,
                     headers: Map<String, String> = emptyMap()): HttpResult {
        var r = raw(method, url, body, contentType, headers, token())
        if (r.code == 401) {
            synchronized(lock) { refreshSession() }
            r = raw(method, url, body, contentType, headers, sp.getString("access", null))
        }
        return r
    }

    private fun authMessage(r: HttpResult): String {
        val j = runCatching { JSONObject(r.text) }.getOrNull()
        val msg = j?.optString("error_description").orEmpty()
            .ifEmpty { j?.optString("msg").orEmpty() }
            .ifEmpty { j?.optString("message").orEmpty() }
        return when {
            msg.contains("Invalid login credentials", ignoreCase = true) -> "อีเมลหรือรหัสผ่านไม่ถูกต้อง"
            msg.contains("Email not confirmed", ignoreCase = true) -> "อีเมลนี้ยังไม่ได้ยืนยัน"
            msg.isNotEmpty() -> msg
            else -> "เข้าสู่ระบบไม่สำเร็จ (รหัส ${r.code})"
        }
    }

    private fun restMessage(r: HttpResult): String {
        val j = runCatching { JSONObject(r.text) }.getOrNull()
        val m = j?.optString("message").orEmpty().ifEmpty { j?.optString("error").orEmpty() }
        return if (m.isNotEmpty()) m else "เซิร์ฟเวอร์ตอบกลับผิดพลาด (รหัส ${r.code})"
    }

    // ---------------- PostgREST ----------------
    private fun rest(path: String) = "${SupabaseConfig.URL}/rest/v1/$path"

    /** ดึงทุกแถว (แบ่งหน้าทีละ 1000) query ต้องมี order เพื่อให้แบ่งหน้าได้ถูกต้อง */
    fun select(table: String, query: String = ""): JSONArray {
        val all = JSONArray()
        var offset = 0
        val page = 1000
        while (true) {
            val q = "select=*" + (if (query.isNotEmpty()) "&$query" else "") + "&limit=$page&offset=$offset"
            val r = call("GET", rest("$table?$q"))
            if (!r.ok) throw ApiException(restMessage(r), r.code)
            val a = JSONArray(r.text)
            for (i in 0 until a.length()) all.put(a.get(i))
            if (a.length() < page) break
            offset += page
        }
        return all
    }

    /** เพิ่มหรืออัปเดตตาม conflict key (ใช้ POST อย่างเดียว ไม่ต้องใช้ PATCH) */
    fun upsert(table: String, rows: JSONArray, onConflict: String = "id", returning: Boolean = true): JSONArray {
        val prefer = "resolution=merge-duplicates," + (if (returning) "return=representation" else "return=minimal")
        val r = call("POST", rest("$table?on_conflict=$onConflict"),
            rows.toString().toByteArray(Charsets.UTF_8), "application/json", mapOf("Prefer" to prefer))
        if (!r.ok) throw ApiException(restMessage(r), r.code)
        return if (returning && r.body.isNotEmpty()) JSONArray(r.text) else JSONArray()
    }

    fun delete(table: String, filter: String) {
        val r = call("DELETE", rest("$table?$filter"))
        if (!r.ok) throw ApiException(restMessage(r), r.code)
    }

    // ---------------- Storage ----------------
    private fun obj(path: String) = "${SupabaseConfig.URL}/storage/v1/object/${SupabaseConfig.BUCKET}/$path"

    fun uploadObject(path: String, bytes: ByteArray, mime: String = "image/jpeg") {
        val r = call("POST", obj(path), bytes, mime, mapOf("x-upsert" to "true"))
        if (!r.ok) throw ApiException(restMessage(r), r.code)
    }

    fun downloadObject(path: String): ByteArray? {
        val r = call("GET", "${SupabaseConfig.URL}/storage/v1/object/authenticated/${SupabaseConfig.BUCKET}/$path")
        return if (r.ok) r.body else null
    }

    fun deleteObject(path: String) {
        call("DELETE", obj(path))
    }
}
