package me.sunzheng.mana.utils;

import me.sunzheng.mana.R;

/**
 * Created by Sun on 2017/5/21.
 */

public interface PreferenceManager {
    interface Global{
        String STR_SP_NAME="global";
        String STR_KEY_HOST="host";
        String INT_KEY_DEFAULT_TIMEOUT="timeout";
        String BOOL_IS_REMEMBERD="rememberd";
        String STR_USERNAME="username";
        String STR_PASSWORD="password";

        // 播放节点路由（mira routing）相关键，见 workdoc/reference/drafts/2026-10-12-播放节点手动选择需求.md §6.1
        String STR_KEY_ROUTING_MODE="routing_mode";
        String STR_KEY_ROUTING_BACKEND_ID="routing_backend_id";
        String STR_KEY_ROUTING_BACKEND_LABEL="routing_backend_label";
        String STR_KEY_ROUTING_CAPABILITY="routing_capability";
        String STR_KEY_ROUTING_CAPABILITY_CHECKED_AT="routing_capability_checked_at";
        String STR_KEY_ROUTING_BACKENDS_JSON="routing_backends_json";

        int RES_JA_FIRST_BOOL = R.string.pref_key_ja_first_bool;
    }

    interface PlayerPolicy {
        String STR_SP_NAME = "policy";
        String BOOL_KEY_AUTOPLAY = "is_autoplay_next";
    }
}
