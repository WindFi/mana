package me.sunzheng.mana.utils

import android.net.Uri
import android.text.TextUtils
import androidx.core.net.toUri

object HostUtil {
    @JvmStatic
    fun makeUp(host: String? = null, url: String? = null): String {
        var _host = if (host!!.endsWith("/"))
            host.substring(0, host.length - 1)
        else host
        var uri = Uri.parse(url)
        return if (!TextUtils.isEmpty(uri.host)) url!! else {
//            host + url
            (_host + url).toUri().toString()
        }
    }
}