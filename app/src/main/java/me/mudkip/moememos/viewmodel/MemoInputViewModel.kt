package me.mudkip.moememos.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skydoves.sandwich.ApiResponse
import com.skydoves.sandwich.suspendOnSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.mudkip.moememos.data.model.Memo
import me.mudkip.moememos.data.model.MemoVisibility
import me.mudkip.moememos.data.model.Resource
import me.mudkip.moememos.data.service.MemoService
import me.mudkip.moememos.ext.settingsDataStore
import me.mudkip.moememos.widget.WidgetUtils
import okhttp3.MediaType.Companion.toMediaType
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MemoInputViewModel @Inject constructor(
    application: android.app.Application,
    @ApplicationContext
    private val context: Context,
    private val memoService: MemoService
) : AndroidViewModel(application) {
    val draft = context.settingsDataStore.data.map { settings ->
        settings.usersList.firstOrNull { it.accountKey == settings.currentUser }?.settings?.draft
    }
    var uploadResources = mutableStateListOf<Resource>()

    suspend fun createMemo(content: String, visibility: MemoVisibility, tags: List<String>): ApiResponse<Memo> = withContext(viewModelScope.coroutineContext) {
        memoService.repository.createMemo(content, visibility, uploadResources, tags).suspendOnSuccess {
            // Refresh widget after creating a new memo
            WidgetUtils.refreshWidgetsOnMemoChange(getApplication())
        }
    }

    suspend fun editMemo(identifier: String, content: String, visibility: MemoVisibility, tags: List<String>): ApiResponse<Memo> = withContext(viewModelScope.coroutineContext) {
        memoService.repository.updateMemo(identifier, content, uploadResources, visibility, tags).suspendOnSuccess {
            // Refresh widget after editing a memo
            WidgetUtils.refreshWidgetsOnMemoUpdate(getApplication(), data)
        }
    }

    fun updateDraft(content: String) = runBlocking {
        context.settingsDataStore.updateData { settings ->
            val currentUser =
                settings.usersList.firstOrNull { it.accountKey == settings.currentUser }
                    ?: return@updateData settings
            val user = currentUser.toBuilder().apply {
                this.settings = this.settings.toBuilder().setDraft(content).build()
            }.build()
            settings.toBuilder().setUsers(settings.usersList.indexOf(currentUser), user).build()
        }
    }

    suspend fun upload(bitmap: Bitmap, memoIdentifier: String?): ApiResponse<Resource> = withContext(viewModelScope.coroutineContext) {
        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bos)
        val bytes = bos.toByteArray()
        memoService.repository.createResource( UUID.randomUUID().toString() + ".jpg", "image/jpeg".toMediaType(), bytes, memoIdentifier).suspendOnSuccess {
            uploadResources.add(data)
        }
    }

    fun deleteResource(resourceIdentifier: String) = viewModelScope.launch {
        memoService.repository.deleteResource(resourceIdentifier).suspendOnSuccess {
            uploadResources.removeIf { it.identifier == resourceIdentifier }
        }
    }
}
