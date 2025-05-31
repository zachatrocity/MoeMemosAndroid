package me.mudkip.moememos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import me.mudkip.moememos.ui.page.common.Navigation
import me.mudkip.moememos.ui.page.common.RouteName
import me.mudkip.moememos.viewmodel.LocalMemos
import me.mudkip.moememos.viewmodel.LocalUserState
import me.mudkip.moememos.viewmodel.MemosViewModel
import me.mudkip.moememos.viewmodel.UserStateViewModel
import me.mudkip.moememos.widget.MoeMemosGlanceWidget

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val userStateViewModel: UserStateViewModel by viewModels()
    private val memosViewModel: MemosViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            CompositionLocalProvider(
                LocalUserState provides userStateViewModel,
                LocalMemos provides memosViewModel
            ) {
                Navigation()
            }
        }
        
        // Handle widget actions
        handleIntent(intent)
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent?) {
        intent?.let {
            when (it.action) {
                MoeMemosGlanceWidget.ACTION_ADD_MEMO -> {
                    // Navigate to the memo input screen
                    val navIntent = Intent(this, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        putExtra("action", "compose")
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    startActivity(navIntent)
                }
                Intent.ACTION_VIEW -> {
                    // Check if we have a memo ID to open
                    val memoId = it.getStringExtra(MoeMemosGlanceWidget.EXTRA_MEMO_ID)
                    if (memoId != null) {
                        // Navigate to the edit screen for this memo
                        val navIntent = Intent(this, MainActivity::class.java).apply {
                            action = Intent.ACTION_VIEW
                            putExtra("route", "${RouteName.EDIT}?memoId=$memoId")
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(navIntent)
                    }
                }
            }
        }
    }
}
