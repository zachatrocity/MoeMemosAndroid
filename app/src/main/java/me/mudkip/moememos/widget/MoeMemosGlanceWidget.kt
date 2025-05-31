package me.mudkip.moememos.widget

import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.skydoves.sandwich.suspendOnSuccess
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.mudkip.moememos.MainActivity
import me.mudkip.moememos.R
import me.mudkip.moememos.data.model.Memo
import me.mudkip.moememos.data.service.MemoService
import javax.inject.Inject

class MoeMemosGlanceWidget : GlanceAppWidget() {
    
    // Define colors to match the app's Material3 theme
    private val backgroundColor = Color(0xFF121212) // Dark background
    private val cardBackgroundColor = Color(0xFF333333) // Dark card background
    private val primaryTextColor = Color(0xFFFFFFFF) // White text
    private val secondaryTextColor = Color(0xFFB0B0B0) // Light gray text
    private val accentColor = Color(0xFF4F6BFF) // Blue accent color
    
    // State for memos
    private val _memos = MutableStateFlow<List<Memo>>(emptyList())
    private val memos: StateFlow<List<Memo>> = _memos.asStateFlow()
    
    fun updateMemos(newMemos: List<Memo>) {
        _memos.value = newMemos
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContent(context)
            }
        }
    }

    @Composable
    private fun WidgetContent(context: Context) {
        val memosList by memos.collectAsState()
        
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(backgroundColor, backgroundColor))
                .padding(12.dp)
        ) {
            // Header with app name and add button
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = context.getString(R.string.app_name),
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(primaryTextColor, primaryTextColor),
                        fontSize = 16.sp
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                
                Image(
                    provider = ImageProvider(R.drawable.ic_add_memo),
                    contentDescription = context.getString(R.string.add_memo),
                    modifier = GlanceModifier
                        .clickable(actionRunCallback<AddMemoAction>())
                )
            }
            
            Spacer(modifier = GlanceModifier.height(8.dp))
            
            // Memo list
            if (memosList.isEmpty()) {
                // Empty state
                Box(
                    modifier = GlanceModifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = context.getString(R.string.no_memos),
                        style = TextStyle(
                            color = ColorProvider(secondaryTextColor, secondaryTextColor),
                            textAlign = TextAlign.Center
                        ),
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    )
                }
            } else {
                // Display memos
                memosList.forEach { memo ->
                    MemoItem(context, memo)
                    Spacer(modifier = GlanceModifier.height(8.dp))
                }
            }
        }
    }
    
    @Composable
    private fun MemoItem(context: Context, memo: Memo) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ColorProvider(cardBackgroundColor, cardBackgroundColor))
                .clickable(actionRunCallback<OpenMemoAction>(
                    parameters = actionParametersOf(
                        MEMO_ID_PARAM to memo.identifier
                    )
                ))
                .padding(10.dp)
        ) {
            Column {
                // Memo content
                Text(
                    text = memo.content,
                    style = TextStyle(
                        color = ColorProvider(primaryTextColor, primaryTextColor),
                        fontSize = 14.sp
                    ),
                    maxLines = 3
                )
                
                Spacer(modifier = GlanceModifier.height(4.dp))
                
                // Memo date
                Text(
                    text = DateUtils.getRelativeTimeSpanString(
                        memo.date.toEpochMilli(),
                        System.currentTimeMillis(),
                        DateUtils.SECOND_IN_MILLIS
                    ).toString(),
                    style = TextStyle(
                        color = ColorProvider(secondaryTextColor, secondaryTextColor),
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
    
    companion object {
        val MEMO_ID_PARAM = ActionParameters.Key<String>("memo_id")
        const val ACTION_ADD_MEMO = "me.mudkip.moememos.action.ADD_MEMO"
        const val ACTION_REFRESH_WIDGET = "me.mudkip.moememos.action.REFRESH_WIDGET"
        const val EXTRA_MEMO_ID = "memo_id"
        
        fun refreshWidgets(context: Context) {
            val intent = Intent(context, MoeMemosGlanceWidgetReceiver::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            context.sendBroadcast(intent)
        }
    }
}

class AddMemoAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = MoeMemosGlanceWidget.ACTION_ADD_MEMO
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }
}

class OpenMemoAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val memoId = parameters[MoeMemosGlanceWidget.MEMO_ID_PARAM] ?: return
        
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(MoeMemosGlanceWidget.EXTRA_MEMO_ID, memoId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }
}
