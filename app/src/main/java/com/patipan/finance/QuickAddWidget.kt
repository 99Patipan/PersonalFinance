package com.patipan.finance

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.patipan.finance.data.EXPENSE
import com.patipan.finance.data.INCOME

/** วิดเจ็ตปุ่มลัด: แตะ "+ รายจ่าย" หรือ "+ รายรับ" เพื่อเปิดแอปที่หน้าเพิ่มรายการทันที */
class QuickAddWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) manager.updateAppWidget(id, build(context))
    }

    private fun build(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_quick_add)
        views.setOnClickPendingIntent(R.id.widget_btn_expense, pending(context, EXPENSE, 1))
        views.setOnClickPendingIntent(R.id.widget_btn_income, pending(context, INCOME, 2))
        return views
    }

    private fun pending(context: Context, type: String, code: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "com.patipan.finance.QUICK_ADD_$type"   // action ต่างกัน เพื่อไม่ให้ PendingIntent สองปุ่มชนกัน
            putExtra(MainActivity.EXTRA_QUICK_ADD, type)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        return PendingIntent.getActivity(context, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
