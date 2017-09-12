package me.sunzheng.mana.home.search;

import android.content.SearchRecentSuggestionsProvider;

/**
 * Created by Sun on 2017/9/11.
 */

public class HistorySuggestionProvider extends SearchRecentSuggestionsProvider {
    public final static String AUTHORITY = "me.sunzheng.mana.historysugestionprovider";
    public final static int MODE = DATABASE_MODE_QUERIES;

    public HistorySuggestionProvider() {
        super();
        setupSuggestions(AUTHORITY, MODE);
    }
}
