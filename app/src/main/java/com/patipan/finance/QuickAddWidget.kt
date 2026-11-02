package com.patipan.finance

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.patipan.finance.data.EXPENSE
import com.patipan.finance.data.INCOME

/** วิดเจ็ตปุ่มลัดขนาด 1 ช่อง: แตะแล้วเปิดแอปที่หน้าเพิ่มรายการประเภทนั้นทันที */
abstract class QuickAddWidget(
    private val type: String,
    private val layoutRes: Int,
    private val requestCode: Int
) : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = RemoteViews(context.packageName, layoutRes)
        views.setOnClickPendingIntent(R.id.widget_root, pending(context))
        for (id in ids) manager.updateAppWidget(id, views)
    }

    private fun pending(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "com.patipan.finance.QUICK_ADD_$type"   // action ต่างกัน เพื่อไม่ให้ PendingIntent ของสองวิดเจ็ตชนกัน
            putExtra(MainActivity.EXTRA_QUICK_ADD, type)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}

class QuickAddExpenseWidget : QuickAddWidget(EXPENSE, R.layout.widget_expense, 1)
class QuickAddIncomeWidget : QuickAddWidget(INCOME, R.layout.widget_income, 2)
