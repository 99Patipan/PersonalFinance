package com.patipan.finance.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Wallet::class, Category::class, Txn::class, Budget::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): FinanceDao

    companion object {
        @Volatile private var inst: AppDatabase? = null

        fun get(ctx: Context): AppDatabase = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(ctx.applicationContext, AppDatabase::class.java, "finance.db")
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) = seed(db)
                })
                .build().also { inst = it }
        }

        // หมวดหมู่และกระเป๋าเริ่มต้น (ไม่ใช่ข้อมูลตัวอย่างของรายการเงิน)
        private fun seed(db: SupportSQLiteDatabase) {
            db.execSQL(
                "INSERT INTO wallets(name,color,initialCents) VALUES(?,?,0)",
                arrayOf<Any>("เงินสด", 0xFFFFC857L)
            )
            val income = listOf("เงินเดือน" to "💰", "OT" to "⏰", "โบนัส" to "🎁",
                "รายได้เสริม" to "💼", "ขายของ" to "🛒", "ค่าคอมมิชชัน" to "🤝", "ดอกเบี้ย" to "🏦",
                "เงินปันผล" to "📈", "เงินคืน/คืนภาษี" to "🧾", "ได้รับเงินจากคนอื่น" to "💵", "อื่น ๆ" to "✨")
            val expense = listOf("อาหาร" to "🍜", "กาแฟและเครื่องดื่ม" to "☕", "ของกินเล่น/ขนม" to "🍰", "ซื้อของเข้าบ้าน" to "🛒",
                "เดินทาง" to "🚌", "แท็กซี่/วิน" to "🚕", "น้ำมันรถ" to "⛽", "ที่จอดรถ/ทางด่วน" to "🅿️", "ซ่อมบำรุงรถ" to "🔧",
                "ที่พัก/ค่าเช่า" to "🏠", "ค่าน้ำและค่าไฟ" to "💡", "โทรศัพท์และอินเทอร์เน็ต" to "📱",
                "ซื้อของ" to "🛍️", "เสื้อผ้า/รองเท้า" to "👕", "ความงาม/ซาลอน" to "💇", "ของใช้ส่วนตัว" to "🧴",
                "สุขภาพ/ยา" to "💊", "โรงพยาบาล/ทันตกรรม" to "🏥", "ฟิตเนส/กีฬา" to "🏋️",
                "การศึกษา/คอร์ส" to "🎓", "หนังสือ" to "📚", "บันเทิง" to "🎮", "ดูหนัง/คอนเสิร์ต" to "🎬",
                "สมาชิกรายเดือน" to "📺", "ท่องเที่ยว" to "✈️", "ของขวัญ/งานสังคม" to "🎁",
                "ลูก/ครอบครัว" to "👶", "สัตว์เลี้ยง" to "🐶", "ทำบุญ/บริจาค" to "🙏",
                "ประกัน" to "🛡️", "ผ่อนชำระ" to "💳", "ออม/ลงทุน" to "📈", "อื่น ๆ" to "📦")
            income.forEach { (n, e) ->
                db.execSQL("INSERT INTO categories(name,type,emoji) VALUES(?,?,?)", arrayOf<Any>(n, INCOME, e))
            }
            expense.forEach { (n, e) ->
                db.execSQL("INSERT INTO categories(name,type,emoji) VALUES(?,?,?)", arrayOf<Any>(n, EXPENSE, e))
            }
        }
    }
}
