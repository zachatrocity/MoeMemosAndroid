package me.mudkip.moememos.widget

import android.content.Context
import me.mudkip.moememos.data.model.Memo

/**
 * Utility class for widget-related operations
 */
object WidgetUtils {
    
    /**
     * Refresh all widgets when a memo is created, updated, or deleted
     */
    fun refreshWidgetsOnMemoChange(context: Context) {
        MoeMemosGlanceWidget.refreshWidgets(context)
    }
    
    /**
     * Refresh all widgets when a specific memo is updated
     */
    fun refreshWidgetsOnMemoUpdate(context: Context, memo: Memo) {
        MoeMemosGlanceWidget.refreshWidgets(context)
    }
}
