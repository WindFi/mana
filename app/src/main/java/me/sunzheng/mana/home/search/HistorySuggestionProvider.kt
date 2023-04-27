package me.sunzheng.mana.home.search

import android.content.SearchRecentSuggestionsProvider

/**
 * Created by Sun on 2017/9/11.
 */
class HistorySuggestionProvider : SearchRecentSuggestionsProvider() {
    init {
        setupSuggestions(AUTHORITY, MODE)
    }

    companion object {
        const val AUTHORITY = "me.sunzheng.mana.historysugestionprovider"
        const val MODE = DATABASE_MODE_QUERIES
    }
}