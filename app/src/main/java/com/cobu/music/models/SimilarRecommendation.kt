package com.cobu.music.models

import com.cobu.innertube.models.YTItem
import com.cobu.music.db.entities.LocalItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
