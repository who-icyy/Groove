package com.cobu.music.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cobu.innertube.YouTube
import com.cobu.innertube.pages.BrowseResult
import com.cobu.music.constants.HideExplicitKey
import com.cobu.music.utils.dataStore
import com.cobu.music.utils.get
import com.cobu.music.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class YouTubeBrowseViewModel @Inject constructor(
    @ApplicationContext context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val browseId = savedStateHandle.get<String>("browseId")!!
    private val params = savedStateHandle.get<String>("params")

    val result = MutableStateFlow<BrowseResult?>(null)

    init {
        viewModelScope.launch {
            YouTube.browse(browseId, params).onSuccess {
                result.value = it.filterExplicit(context.dataStore.get(HideExplicitKey, false))
            }.onFailure {
                reportException(it)
            }
        }
    }
}
