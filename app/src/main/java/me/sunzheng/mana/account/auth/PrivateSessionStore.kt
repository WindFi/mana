package me.sunzheng.mana.account.auth

import android.content.SharedPreferences
import java.net.HttpCookie

/**
 * 「会话凭据：私有存储与迁移」领域（2026-10-12 结构归集）。
 *
 * 原 `SharedPreferencesSessionStore.kt` + `SessionMigrator.kt` 合到本文件，并把原先分离的
 * 「一次性迁移器」与「主后端 + 兜底后端装饰器」**合成一个类** [MigratingSessionStore]：
 * 少一层间接、少一个类，迁移语义（先校验、后清除）逐字保留。
 */

/**
 * 应用私有存储（`CookiePrefsFile`）后端 —— 改造前 [me.sunzheng.mana.account.auth.PersistentHttpCookieStore]
 * 的 SP 逻辑**原样下沉**到这里，键布局逐字不变（否则升级用户读不到老数据）：
 *
 * ```
 * url                -> Set<origin>          // 有 cookie 的 origin 索引
 * <origin>           -> Set<origin+name>     // 该 origin 下 cookie 的键索引
 * <origin+name>      -> String               // 该 cookie 的值（JSON；老数据为 Parcel+Base64）
 * ```
 *
 * 读取会先按 JSON 解、再回退老格式（见 [CompatCookieCodec]）；写入统一是 JSON。
 */
class SharedPreferencesSessionStore(
    private val prefs: SharedPreferences
) : SessionStore {

    companion object {
        /** SP 文件名，与改造前一致。 */
        const val PREFS_NAME = "CookiePrefsFile"

        /** origin 索引键名，与改造前一致。 */
        const val KEY_URI_INDEX = "url"

        /** 某个 origin 下 cookie 键的拼法，与改造前一致。 */
        fun cookieKey(origin: String, cookieName: String): String = origin + cookieName
    }

    override fun read(origin: String): List<HttpCookie> {
        val keys = prefs.getStringSet(origin, emptySet()) ?: return emptyList()
        if (keys.isEmpty()) {
            return emptyList()
        }
        val result = ArrayList<HttpCookie>(keys.size)
        for (key in keys) {
            result += CompatCookieCodec.decode(prefs.getString(key, null))
        }
        return result
    }

    override fun write(origin: String, cookies: List<HttpCookie>): Boolean {
        if (cookies.isEmpty()) {
            delete(origin)
            return true
        }
        return try {
            val editor = prefs.edit()
            val previous = prefs.getStringSet(origin, emptySet()) ?: emptySet()
            val next = HashSet<String>(cookies.size)
            for (cookie in cookies) {
                val key = cookieKey(origin, cookie.name)
                next += key
                editor.putString(key, CompatCookieCodec.encode(listOf(cookie)))
            }
            // 覆盖写：该 origin 下已不存在的 cookie 键要一起清掉，避免残值被再次读出来。
            for (key in previous) {
                if (key !in next) {
                    editor.remove(key)
                }
            }
            editor.putStringSet(origin, next)

            val index = HashSet(prefs.getStringSet(KEY_URI_INDEX, emptySet()) ?: emptySet())
            index += origin
            editor.putStringSet(KEY_URI_INDEX, index)
            // apply()：SP 的内存缓存同步更新、落盘异步，读回校验立刻可见，且不阻塞 OkHttp 线程。
            editor.apply()
            true
        } catch (e: RuntimeException) {
            false
        }
    }

    override fun delete(origin: String) {
        val editor = prefs.edit()
        for (key in prefs.getStringSet(origin, emptySet()) ?: emptySet()) {
            editor.remove(key)
        }
        editor.remove(origin)

        val index = HashSet(prefs.getStringSet(KEY_URI_INDEX, emptySet()) ?: emptySet())
        if (index.remove(origin)) {
            editor.putStringSet(KEY_URI_INDEX, index)
        }
        editor.apply()
    }

    override fun origins(): Set<String> =
        (prefs.getStringSet(KEY_URI_INDEX, emptySet()) ?: emptySet()).toSet()

    override fun clear() {
        prefs.edit().clear().apply()
    }
}

/** 迁移决策结果（任务 3.1 的决策表）。 */
internal enum class MigrationOutcome {
    /** 目标后端已有该 origin 的凭据，无需搬运。 */
    TARGET_PRESENT,

    /** 两个后端都没有该 origin 的凭据。 */
    SOURCE_EMPTY,

    /** 写入目标后端失败（例如账号库写不进，已由目标实现降级），保留旧数据。 */
    WRITE_FAILED,

    /** 写入成功但读回不一致，保留旧数据。 */
    VERIFY_FAILED,

    /** 搬迁成功且读回校验一致，旧数据已清除。 */
    MOVED
}

/**
 * 主后端（账号库）+ 兜底后端（SP）的组合，并把一次性迁移内联进来（design D4/D6/D10）。
 *
 * - `read`：主后端有就用主后端；否则**先尝试一次迁移**（每个 origin 每进程最多一次）再兜底 ——
 *   这保证「先迁移、后判据」的顺序（升级用户在判据读取前完成搬迁，不会掉登录）；
 * - `write`：主后端写失败即降级兜底（并返回兜底结果）；
 * - `delete`：两处都清（登出必须两端都不残留）；
 * - [migrate]：**先校验、后清除** —— 主后端无该 origin 且兜底有 → 写主后端 → 读回比对（逐字段等价）
 *   → 一致才删兜底里该 origin 的键；不一致或写入失败则保留兜底数据并继续用兜底。
 *   整个过程不抛异常（唯一失败语义是 [MigrationOutcome]）。
 */
class MigratingSessionStore(
    private val primary: SessionStore,
    private val fallback: SessionStore,
    private val warn: (String) -> Unit = {}
) : SessionStore {

    private val migrationAttempted = HashSet<String>()
    private val lock = Any()

    override fun read(origin: String): List<HttpCookie> {
        val fromPrimary = primary.read(origin)
        if (fromPrimary.isNotEmpty()) {
            return fromPrimary
        }
        if (markMigrationAttempted(origin)) {
            when (migrate(origin)) {
                MigrationOutcome.MOVED, MigrationOutcome.TARGET_PRESENT -> {
                    val migrated = primary.read(origin)
                    if (migrated.isNotEmpty()) {
                        return migrated
                    }
                }

                else -> Unit
            }
        }
        return fallback.read(origin)
    }

    override fun write(origin: String, cookies: List<HttpCookie>): Boolean {
        if (primary.write(origin, cookies)) {
            return true
        }
        return fallback.write(origin, cookies)
    }

    override fun delete(origin: String) {
        primary.delete(origin)
        fallback.delete(origin)
    }

    override fun origins(): Set<String> = primary.origins() + fallback.origins()

    override fun clear() {
        primary.clear()
        fallback.clear()
    }

    /** 把主后端该 origin 的凭据搬进唯一真源；可重复调用，幂等。 */
    internal fun migrate(origin: String): MigrationOutcome = synchronized(lock) {
        if (primary.read(origin).isNotEmpty()) {
            return@synchronized MigrationOutcome.TARGET_PRESENT
        }
        val source = fallback.read(origin)
        if (source.isEmpty()) {
            return@synchronized MigrationOutcome.SOURCE_EMPTY
        }
        if (!primary.write(origin, source)) {
            warn("会话凭据迁移失败（写入目标后端失败），保留私有存储副本：origin=$origin")
            return@synchronized MigrationOutcome.WRITE_FAILED
        }
        val readBack = primary.read(origin)
        if (!equivalentCookies(source, readBack)) {
            warn("会话凭据迁移校验不一致（读回与源不等价），保留私有存储副本：origin=$origin")
            return@synchronized MigrationOutcome.VERIFY_FAILED
        }
        fallback.delete(origin)
        MigrationOutcome.MOVED
    }

    private fun markMigrationAttempted(origin: String): Boolean = synchronized(lock) {
        migrationAttempted.add(origin)
    }
}
