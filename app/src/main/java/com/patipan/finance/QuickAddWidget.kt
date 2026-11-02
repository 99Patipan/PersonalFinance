package com.patipan.finance

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.patipan.finance.data.EXPENSE
import com.patipan.finance.data.INCOME
import com.patipan.finance.data.Prefs

/**
 * วิดเจ็ตปุ่มลัดขนาด 1 ช่อง
 * กดแล้ว → บันทึกประเภทลง Prefs ก่อน → เปิด MainActivity
 * (ไม่พึ่ง Intent extra ของ Activity เพราะบนบางเครื่อง extra หายตอน singleTop)
 */
abstract class QuickAddWidget(
    private val type: String,
    private val layoutRes: Int,
    private val requestCode: Int
) : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = RemoteViews(context.packageName, layoutRes)
        val click = clickPending(context)
        views.setOnClickPendingIntent(R.id.widget_root, click)
        views.setOnClickPendingIntent(R.id.widget_icon, click)
        views.setOnClickPendingIntent(R.id.widget_label, click)
        for (id in ids) manager.updateAppWidget(id, views)
    }

    override fun onReceive(context: Context, intent: Intent) {
        // ต้องเรียก super เพื่อให้ onUpdate / onEnabled ทำงานตามปกติ
        super.onReceive(context, intent)
        if (intent.action != ACTION_QUICK_ADD_CLICK) return
        val t = intent.getStringExtra(EXTRA_TYPE) ?: return
        if (t != EXPENSE && t != INCOME) return

        // 1) บันทึกประเภทลง Prefs ทันที (sync) — แหล่งความจริงหลัก
        Prefs(context.applicationContext).setPendingQuickAdd(t)

        // 2) เปิดแอป (ไม่ต้องพึ่ง extra ของ Activity อีกต่อไป)
        val launch = Intent(context, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
            // ใส่ไว้เผื่อเครื่องที่ extra ยังมาถึง
            putExtra(MainActivity.EXTRA_QUICK_ADD, t)
            action = "com.patipan.finance.OPEN_QUICK_ADD"
        }
        context.startActivity(launch)
    }

    private fun clickPending(context: Context): PendingIntent {
        val intent = Intent(context, this::class.java).apply {
            action = ACTION_QUICK_ADD_CLICK
            putExtra(EXTRA_TYPE, type)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_QUICK_ADD_CLICK = "com.patipan.finance.ACTION_QUICK_ADD_CLICK"
        const val EXTRA_TYPE = "quick_add_type"
    }
}

class QuickAddExpenseWidget : QuickAddWidget(EXPENSE, R.layout.widget_expense, 1)
class QuickAddIncomeWidget : QuickAddWidget(INCOME, R.layout.widget_income, 2)
