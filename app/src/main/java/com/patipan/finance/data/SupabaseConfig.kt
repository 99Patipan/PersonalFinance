package com.patipan.finance.data

/**
 * ค่าเชื่อมต่อ Supabase (publishable key ออกแบบมาให้อยู่ในแอปได้ ความปลอดภัยของข้อมูลมาจาก RLS + การล็อกอิน)
 * ถ้าเชื่อมต่อด้วยคีย์นี้ไม่ได้ ให้ลองเปลี่ยน API_KEY เป็น legacy anon key จากหน้า Settings > API Keys ของ Supabase
 */
object SupabaseConfig {
    const val URL = "https://amflmbejnivwxojjwdyf.supabase.co"
    const val API_KEY = "sb_publishable_O8tPCbW78SnBO4ynJ0CzzA_LDX3U8Zo"
    const val BUCKET = "receipts"
}
