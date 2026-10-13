package me.sunzheng.mana.account.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/* Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * A cookie store backed by a pluggable {@link SessionStore}.
 * <p>
 * Forked from https://github.com/floating-cat/S1-Next/blob/0abbb2fcd7e390208fbd793c8bdda140e7d41865/app/src/main/java/cl/monsoon/s1next/widget/PersistentHttpCookieStore.java
 * I'm so lazy....
 * -----by Sun
 * <p>
 * 2026-10 改造（openspec `accountmanager-session-store`）：cookie 的匹配/过期/清理语义**逐字保留**，
 * 只把「持久化到哪」抽成 {@link SessionStore} —— 账号库（API ≥ 23）或应用私有存储（SP）。
 * 之前的 `CookiePrefsFile` 写入逻辑已下沉到
 * {@code me.sunzheng.mana.account.auth.SharedPreferencesSessionStore}，编码从 `Parcel + Base64`
 * 换成了 JSON（老数据仍可读，见 `CompatCookieCodec`）。
 * <p>
 * 内存 {@link #map} 仍是读路径的主来源；构造时不读存储（避免在主线程做账号库 IPC 与迁移），
 * 首次使用（OkHttp 的 cookie 线程）才惰性加载。
 */
public class PersistentHttpCookieStore implements CookieStore {

    @NonNull
    private final SessionStore sessionStore;

    /**
     * This map may have null keys!
     */
    @NonNull
    private final Map<URI, List<HttpCookie>> map = new HashMap<>();

    private boolean loaded = false;

    public PersistentHttpCookieStore(@NonNull SessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    /**
     * 惰性加载：把各 origin 的持久化凭据读进内存。必须在 synchronized 方法内调用。
     * <p>
     * 首次读取会触发 SessionStore 侧的一次性迁移（先迁移、后判据），因此这里可能有一次账号库 IPC；
     * 调用方是 OkHttp 的 cookie 线程，不是主线程。
     */
    private void ensureLoadedLocked() {
        if (loaded) {
            return;
        }
        loaded = true;
        for (String origin : sessionStore.origins()) {
            URI uri = URI.create(origin);
            List<HttpCookie> cookies = map.get(uri);
            if (cookies == null) {
                cookies = new ArrayList<>();
                map.put(uri, cookies);
            }
            for (HttpCookie cookie : sessionStore.read(origin)) {
                if (!cookies.contains(cookie)) {
                    cookies.add(cookie);
                }
            }
        }
    }

    /** 把该 origin 的内存态写回存储层（覆盖写；空列表即删除）。 */
    private void persistLocked(@Nullable URI uri, @NonNull List<HttpCookie> cookies) {
        if (uri == null) {
            // 无 host 的 URI 没有可归属的 origin，与改造前一致：只留在内存里。
            return;
        }
        if (cookies.isEmpty()) {
            sessionStore.delete(uri.toString());
        } else {
            sessionStore.write(uri.toString(), new ArrayList<>(cookies));
        }
    }

    @Override
    public synchronized void add(URI uri, @Nullable HttpCookie httpCookie) {
        if (httpCookie == null) {
            throw new NullPointerException("cookie == null");
        }
        ensureLoadedLocked();

        uri = cookiesUri(uri);
        List<HttpCookie> cookies = map.get(uri);
        if (cookies == null) {
            cookies = new ArrayList<>();
            map.put(uri, cookies);
        } else {
            cookies.remove(httpCookie);
        }
        cookies.add(httpCookie);

        persistLocked(uri, cookies);
    }

    private URI cookiesUri(@Nullable URI uri) {
        if (uri == null) {
            return null;
        }

        try {
            return new URI("http", uri.getHost(), null, null);
        } catch (URISyntaxException e) {
            return uri; // probably a URI with no host
        }
    }

    @NonNull
    @Override
    public synchronized List<HttpCookie> get(@Nullable URI uri) {
        if (uri == null) {
            throw new NullPointerException("uri == null");
        }
        ensureLoadedLocked();

        List<HttpCookie> result = new ArrayList<>();

        // get cookies associated with given URI. If none, returns an empty list
        List<HttpCookie> cookiesForUri = map.get(uri);
        if (cookiesForUri != null) {
            for (Iterator<HttpCookie> i = cookiesForUri.iterator(); i.hasNext(); ) {
                HttpCookie cookie = i.next();
                if (cookie.hasExpired()) {
                    i.remove(); // remove expired cookies
                } else {
                    result.add(cookie);
                }
            }
        }

        // get all cookies that domain matches the URI
        for (Map.Entry<URI, List<HttpCookie>> entry : map.entrySet()) {
            if (uri.equals(entry.getKey())) {
                continue; // skip the given URI; we've already handled it
            }
            List<HttpCookie> entryCookies = entry.getValue();
            for (Iterator<HttpCookie> i = entryCookies.iterator(); i.hasNext(); ) {
                HttpCookie cookie = i.next();
                if (!HttpCookie.domainMatches(cookie.getDomain(), uri.getHost())) {
                    continue;
                }
                if (cookie.hasExpired()) {
                    i.remove(); // remove expired cookies
                } else if (!result.contains(cookie)) {
                    result.add(cookie);
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    @NonNull
    @Override
    public synchronized List<HttpCookie> getCookies() {
        ensureLoadedLocked();
        List<HttpCookie> result = new ArrayList<>();
        for (List<HttpCookie> list : map.values()) {
            for (Iterator<HttpCookie> i = list.iterator(); i.hasNext(); ) {
                HttpCookie cookie = i.next();
                if (cookie.hasExpired()) {
                    i.remove(); // remove expired cookies
                } else if (!result.contains(cookie)) {
                    result.add(cookie);
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    @NonNull
    @Override
    public synchronized List<URI> getURIs() {
        ensureLoadedLocked();
        List<URI> result = new ArrayList<>(map.keySet());
        result.remove(null); // sigh
        return Collections.unmodifiableList(result);
    }

    @Override
    public synchronized boolean remove(URI uri, @Nullable HttpCookie httpCookie) {
        if (httpCookie == null) {
            throw new NullPointerException("cookie == null");
        }
        ensureLoadedLocked();

        uri = cookiesUri(uri);
        List<HttpCookie> cookies = map.get(uri);
        if (cookies != null) {
            boolean removed = cookies.remove(httpCookie);
            if (removed) {
                persistLocked(uri, cookies);
            }
            return removed;
        } else {
            return false;
        }
    }

    @Override
    public synchronized boolean removeAll() {
        ensureLoadedLocked();

        // clear cookies from the persistence backend
        sessionStore.clear();

        // clear cookies from local store
        boolean result = !map.isEmpty();
        map.clear();
        return result;
    }

    /**
     * 清除某个 origin（归一化键，如 {@code http://host}）的会话：内存 map + 持久化后端（账号库 + 私有存储残留）。
     *
     * 登录态判据的清理在 {@code SessionLogout.clearSession} 里统一做；这里只负责「本次进程不再携带该 cookie」。
     *
     * @return 内存里是否确实清掉了 cookie（用于判断本次清理是否产生了副作用）
     */
    public synchronized boolean clearForOrigin(@NonNull String origin) {
        List<HttpCookie> removed = map.remove(URI.create(origin));
        // 先删存储：即使本次还没惰性加载内存，之后的加载也不会把被删凭据复活。
        sessionStore.delete(origin);
        return removed != null && !removed.isEmpty();
    }
}
