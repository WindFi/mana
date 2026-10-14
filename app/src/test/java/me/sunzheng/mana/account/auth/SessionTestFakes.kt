package me.sunzheng.mana.account.auth

import android.content.SharedPreferences
import java.net.HttpCookie

/**
 * 内存版 [SessionStore]，用于覆盖迁移/降级/登出等决策表：
 * 可以通过 [failWrite] / [corruptOnWrite] / [readFailure] 精确模拟账号库的失败形态，
 * 这些正是真机才能触发的分支（本工程无 Robolectric）。
 */
class FakeSessionStore(
    private val cookies: MutableMap<String, List<HttpCookie>> = HashMap(),
    var failWrite: Boolean = false,
    var corruptOnWrite: Boolean = false,
    var readFailure: Boolean = false,
    /** 模拟「原始数据还在、但一条 cookie 都解不出来」（design D13 结论 2）。 */
    val undecodableOrigins: Set<String> = emptySet()
) : SessionStore {

    var writeCount = 0
        private set

    override fun read(origin: String): List<HttpCookie> =
        if (readFailure || origin in undecodableOrigins) emptyList() else cookies[origin].orEmpty()

    override fun hasUndecodableData(origin: String): Boolean = origin in undecodableOrigins

    override fun write(origin: String, cookies: List<HttpCookie>): Boolean {
        writeCount++
        if (failWrite) {
            return false
        }
        if (cookies.isEmpty()) {
            this.cookies.remove(origin)
            return true
        }
        this.cookies[origin] = if (corruptOnWrite) {
            cookies.map { HttpCookie(it.name, "corrupted-by-fake") }
        } else {
            ArrayList(cookies)
        }
        return true
    }

    override fun delete(origin: String) {
        cookies.remove(origin)
    }

    override fun origins(): Set<String> = cookies.keys.toSet()

    override fun clear() {
        cookies.clear()
    }

    fun contains(origin: String): Boolean = cookies.containsKey(origin)
}

/**
 * 内存版 [SharedPreferences]（含 `Editor` 的暂存语义），用来在纯 JVM 单测里覆盖
 * 老 `CookiePrefsFile` 的键布局 —— 该接口是 Android 的**接口**，实现它不需要框架。
 */
class FakeSharedPreferences(
    private val values: MutableMap<String, Any?> = HashMap()
) : SharedPreferences {

    override fun getAll(): MutableMap<String, *> = HashMap(values)

    override fun getString(key: String?, defValue: String?): String? =
        values[key] as? String ?: defValue

    override fun getStringSet(key: String?, defValues: Set<String>?): Set<String>? =
        (values[key] as? Set<*>)?.filterIsInstance<String>()?.toSet() ?: defValues

    override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue

    override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        values[key] as? Boolean ?: defValue

    override fun contains(key: String?): Boolean = values.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) = Unit

    inner class FakeEditor : SharedPreferences.Editor {

        private val staged = HashMap<String, Any?>()
        private val removed = HashSet<String>()
        private var clearAll = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            staged[key!!] = value
            return this
        }

        override fun putStringSet(
            key: String?,
            values: MutableSet<String>?
        ): SharedPreferences.Editor {
            staged[key!!] = values?.toMutableSet()
            return this
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            staged[key!!] = value
            return this
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            staged[key!!] = value
            return this
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            staged[key!!] = value
            return this
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            staged[key!!] = value
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            removed += key!!
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clearAll = true
            return this
        }

        override fun commit(): Boolean {
            flush()
            return true
        }

        override fun apply() {
            flush()
        }

        private fun flush() {
            if (clearAll) {
                values.clear()
                clearAll = false
            }
            removed.forEach { values.remove(it) }
            removed.clear()
            values.putAll(staged)
            staged.clear()
        }
    }
}

/** 造一个带全部属性的 cookie，用于编解码/迁移的等价性断言。 */
fun cookie(
    name: String,
    value: String,
    domain: String? = "example.com",
    path: String? = "/",
    maxAge: Long = 2592000L,
    secure: Boolean = false
): HttpCookie = HttpCookie(name, value).apply {
    this.domain = domain
    this.path = path
    this.maxAge = maxAge
    this.secure = secure
    comment = "comment"
    commentURL = "https://example.com/comment"
    portlist = "80"
    discard = true
    version = 1
}
