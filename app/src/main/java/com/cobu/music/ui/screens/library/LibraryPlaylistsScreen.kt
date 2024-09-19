package com.cobu.music.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.cobu.music.LocalDatabase
import com.cobu.music.LocalPlayerAwareWindowInsets
import com.cobu.music.R
import com.cobu.music.constants.CONTENT_TYPE_HEADER
import com.cobu.music.constants.CONTENT_TYPE_PLAYLIST
import com.cobu.music.constants.GridCellSize
import com.cobu.music.constants.GridCellSizeKey
import com.cobu.music.constants.GridThumbnailHeight
import com.cobu.music.constants.LibraryViewType
import com.cobu.music.constants.PlaylistSortDescendingKey
import com.cobu.music.constants.PlaylistSortType
import com.cobu.music.constants.PlaylistSortTypeKey
import com.cobu.music.constants.PlaylistViewTypeKey
import com.cobu.music.constants.SmallGridThumbnailHeight
import com.cobu.music.db.entities.PlaylistEntity
import com.cobu.music.ui.component.EmptyPlaceholder
import com.cobu.music.ui.component.HideOnScrollFAB
import com.cobu.music.ui.component.LocalMenuState
import com.cobu.music.ui.component.PlaylistGridItem
import com.cobu.music.ui.component.PlaylistListItem
import com.cobu.music.ui.component.SortHeader
import com.cobu.music.ui.component.TextFieldDialog
import com.cobu.music.ui.menu.PlaylistMenu
import com.cobu.music.utils.rememberEnumPreference
import com.cobu.music.utils.rememberPreference
import com.cobu.music.viewmodels.LibraryPlaylistsViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryPlaylistsScreen(
    navController: NavController,
    viewModel: LibraryPlaylistsViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current

    val coroutineScope = rememberCoroutineScope()

    val gridCellSize by rememberEnumPreference(GridCellSizeKey, GridCellSize.SMALL)
    var viewType by rememberEnumPreference(PlaylistViewTypeKey, LibraryViewType.GRID)
    val (sortType, onSortTypeChange) = rememberEnumPreference(PlaylistSortTypeKey, PlaylistSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(PlaylistSortDescendingKey, true)

    val playlists by viewModel.allPlaylists.collectAsState()

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            when (viewType) {
                LibraryViewType.LIST -> lazyListState.animateScrollToItem(0)
                LibraryViewType.GRID -> lazyGridState.animateScrollToItem(0)
            }
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    var showAddPlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showAddPlaylistDialog) {
        TextFieldDialog(
            icon = { Icon(painter = painterResource(R.drawable.add), contentDescription = null) },
            title = { Text(text = stringResource(R.string.create_playlist)) },
            onDismiss = { showAddPlaylistDialog = false },
            onDone = { playlistName ->
                database.query {
                    insert(
                        PlaylistEntity(
                            name = playlistName
                        )
                    )
                }
            }
        )
    }

    val headerContent = @Composable {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp)
        ) {
            SortHeader(
                sortType = sortType,
                sortDescending = sortDescending,
                onSortTypeChange = onSortTypeChange,
                onSortDescendingChange = onSortDescendingChange,
                sortTypeText = { sortType ->
                    when (sortType) {
                        PlaylistSortType.CREATE_DATE -> R.string.sort_by_create_date
                        PlaylistSortType.NAME -> R.string.sort_by_name
                        PlaylistSortType.SONG_COUNT -> R.string.sort_by_song_count
                    }
                }
            )

            Spacer(Modifier.weight(1f))

            playlists?.let { playlists ->
                Text(
                    text = pluralStringResource(R.plurals.n_playlist, playlists.size, playlists.size),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            IconButton(
                onClick = {
                    viewType = viewType.toggle()
                },
                modifier = Modifier.padding(start = 6.dp, end = 6.dp)
            ) {
                Icon(
                    painter = painterResource(
                        when (viewType) {
                            LibraryViewType.LIST -> R.drawable.list
                            LibraryViewType.GRID -> R.drawable.grid_view
                        }
                    ),
                    contentDescription = null
                )
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (viewType) {
            LibraryViewType.LIST -> {
                LazyColumn(
                    state = lazyListState,
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
                ) {
                    item(
                        key = "header",
                        contentType = CONTENT_TYPE_HEADER
                    ) {
                        headerContent()
                    }

                    playlists?.let { playlists ->
                        if (playlists.isEmpty()) {
                            item {
                                EmptyPlaceholder(
                                    icon = R.drawable.queue_music,
                                    text = stringResource(R.string.library_playlist_empty),
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }

                        items(
                            items = playlists,
                            key = { it.id },
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) { playlist ->
                            PlaylistListItem(
                                playlist = playlist,
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                PlaylistMenu(
                                                    playlist = playlist,
                                                    coroutineScope = coroutineScope,
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.more_vert),
                                            contentDescription = null
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate("local_playlist/${playlist.id}")
                                    }
                                    .animateItem()
                            )
                        }
                    }
                }

                HideOnScrollFAB(
                    lazyListState = lazyListState,
                    icon = R.drawable.add,
                    onClick = {
                        showAddPlaylistDialog = true
                    }
                )
            }

            LibraryViewType.GRID -> {
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns = GridCells.Adaptive(
                        minSize = when (gridCellSize) {
                            GridCellSize.SMALL -> SmallGridThumbnailHeight
                            GridCellSize.BIG -> GridThumbnailHeight
                        } + 24.dp
                    ),
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
                ) {
                    item(
                        key = "header",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER
                    ) {
                        headerContent()
                    }

                    playlists?.let { playlists ->
                        if (playlists.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                EmptyPlaceholder(
                                    icon = R.drawable.queue_music,
                                    text = stringResource(R.string.library_playlist_empty),
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }

                        items(
                            items = playlists,
                            key = { it.id },
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) { playlist ->
                            PlaylistGridItem(
                                playlist = playlist,
                                fillMaxWidth = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("local_playlist/${playlist.id}")
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                PlaylistMenu(
                                                    playlist = playlist,
                                                    coroutineScope = coroutineScope,
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                        }
                                    )
                                    .animateItem()
                            )
                        }
                    }
                }

                HideOnScrollFAB(
                    lazyListState = lazyGridState,
                    icon = R.drawable.add,
                    onClick = {
                        showAddPlaylistDialog = true
                    }
                )
            }
        }

    }
}
