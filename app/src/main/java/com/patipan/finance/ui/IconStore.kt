package com.patipan.finance.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ไอคอนหมวดหมู่จากรูปใน assets/icons/<กลุ่ม>/<ชื่อ>.webp
 * เก็บลงคอลัมน์ emoji ของตาราง categories เป็นข้อความรูปแบบ "icon:<กลุ่ม>/<ชื่อ>" (ไม่ต้องแก้โครงสร้างตาราง)
 * ส่วนหมวดเดิมที่เป็นอีโมจิยังแสดงได้ตามปกติ
 */
object IconStore {
    const val PREFIX = "icon:"

    class Group(val key: String, val title: String)

    /** กลุ่มไอคอน ตรงกับโฟลเดอร์ในไฟล์ icon.zip */
    val groups = listOf(
        Group("base", "หมวดยอดนิยม"),
        Group("food", "อาหารและเครื่องดื่ม"),
        Group("biz", "การเงินและธุรกิจ"),
        Group("shop", "ช้อปปิ้งและร้านค้า"),
        Group("ship", "จัดส่งและขนส่ง"),
        Group("med", "การแพทย์และสุขภาพ"),
        Group("tools", "เครื่องมือและก่อสร้าง"),
        Group("fashion", "แฟชั่นและเสื้อผ้า"),
        Group("edu", "การศึกษา"),
        Group("summer", "ท่องเที่ยวและกิจกรรม"),
        Group("nature", "ธรรมชาติและพลังงาน"),
        Group("data", "ข้อมูลและการวิเคราะห์"),
        Group("tech", "เทคโนโลยี"),
        Group("elec", "เครื่องใช้ไฟฟ้า"),
        Group("family", "ครอบครัว"),
        Group("teen", "วัยรุ่น")
    )

    val defaultExpense = ref("base", "shopping_bag")
    val defaultIncome = ref("base", "money_bag")

    fun ref(group: String, name: String) = "$PREFIX$group/$name"
    fun isIcon(s: String) = s.startsWith(PREFIX)
    fun groupOf(ref: String): String? =
        if (isIcon(ref)) ref.removePrefix(PREFIX).substringBefore('/').takeIf { k -> groups.any { it.key == k } } else null

    private val cache = LruCache<String, ImageBitmap>(500)
    private val nameCache = HashMap<String, List<String>>()

    fun names(ctx: Context, group: String): List<String> = synchronized(nameCache) {
        nameCache.getOrPut(group) {
            ctx.applicationContext.assets.list("icons/$group")
                ?.filter { it.endsWith(".webp") }?.map { it.removeSuffix(".webp") }?.sorted() ?: emptyList()
        }
    }

    fun cached(ref: String): ImageBitmap? = cache.get(ref)

    fun load(ctx: Context, ref: String): ImageBitmap? {
        cache.get(ref)?.let { return it }
        val path = "icons/" + ref.removePrefix(PREFIX) + ".webp"
        val bmp = runCatching {
            ctx.applicationContext.assets.open(path).use { BitmapFactory.decodeStream(it) }
        }.getOrNull()?.asImageBitmap() ?: return null
        cache.put(ref, bmp)
        return bmp
    }
}

/** ข้อความอีโมจิของหมวด (ถ้าเป็นไอคอนรูปภาพ จะไม่มีตัวอักษรให้แสดง) */
fun iconText(icon: String) = if (IconStore.isIcon(icon)) "" else icon

/** แสดงไอคอนหมวดหมู่: ถ้าเป็น "icon:..." วาดรูปจาก assets ไม่งั้นแสดงเป็นอีโมจิ */
@Composable
fun CategoryIcon(icon: String, size: Dp, emojiSize: TextUnit = (size.value * 0.78f).sp) {
    if (!IconStore.isIcon(icon)) {
        Box(Modifier.size(size), contentAlignment = Alignment.Center) {
            Text(icon.ifBlank { "🏷️" }, fontSize = emojiSize)
        }
        return
    }
    val ctx = LocalContext.current
    val bmp by produceState<ImageBitmap?>(IconStore.cached(icon), icon) {
        if (value == null) value = withContext(Dispatchers.IO) { IconStore.load(ctx, icon) }
    }
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        bmp?.let { Image(it, contentDescription = null, modifier = Modifier.size(size)) }
    }
}
