package me.mudkip.moememos.widget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.skydoves.sandwich.suspendOnSuccess
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import me.mudkip.moememos.data.service.MemoService
import javax.inject.Inject

@AndroidEntryPoint
class MoeMemosGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    
    @Inject
    lateinit var memoService: MemoService
    
    private val coroutineScope = MainScope()
    private val glanceWidget = MoeMemosGlanceWidget()
    
    override val glanceAppWidget: GlanceAppWidget = glanceWidget
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        
        // Fetch memos and update the widget
        coroutineScope.launch {
            updateWidgetWithMemos(context)
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        if (intent.action == MoeMemosGlanceWidget.ACTION_REFRESH_WIDGET) {
            coroutineScope.launch {
                updateWidgetWithMemos(context)
            }
        }
    }
    
    private suspend fun updateWidgetWithMemos(context: Context) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                memoService.repository.listMemos().suspendOnSuccess {
                    val memos = this.data.take(3)
                    glanceWidget.updateMemos(memos)
                    
                    // Update all instances of the widget
                    val manager = android.appwidget.AppWidgetManager.getInstance(context)
                    val ids = manager.getAppWidgetIds(
                        android.content.ComponentName(context, MoeMemosGlanceWidgetReceiver::class.java)
                    )
                    
                    // Force update all widget instances
                    ids.forEach { widgetId ->
                        manager.updateAppWidget(widgetId, null)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
