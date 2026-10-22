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
                "รายได้เสริม" to "💼", "ดอกเบี้ย" to "🏦", "อื่น ๆ" to "✨")
            val expense = listOf("อาหาร" to "🍜", "เดินทาง" to "🚌", "น้ำมันรถ" to "⛽", "ที่พัก" to "🏠",
                "ค่าน้ำและค่าไฟ" to "💡", "โทรศัพท์และอินเทอร์เน็ต" to "📱", "ซื้อของ" to "🛍️",
                "สุขภาพ" to "💊", "บันเทิง" to "🎮", "ผ่อนชำระ" to "💳", "อื่น ๆ" to "📦")
            income.forEach { (n, e) ->
                db.execSQL("INSERT INTO categories(name,type,emoji) VALUES(?,?,?)", arrayOf<Any>(n, INCOME, e))
            }
            expense.forEach { (n, e) ->
                db.execSQL("INSERT INTO categories(name,type,emoji) VALUES(?,?,?)", arrayOf<Any>(n, EXPENSE, e))
            }
        }
    }
}
