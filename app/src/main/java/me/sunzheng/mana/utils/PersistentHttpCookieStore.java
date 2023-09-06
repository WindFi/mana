package me.sunzheng.mana.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * A shared preference cookie store.
 * <p>
 * Forked from https://github.com/floating-cat/S1-Next/blob/0abbb2fcd7e390208fbd793c8bdda140e7d41865/app/src/main/java/cl/monsoon/s1next/widget/PersistentHttpCookieStore.java
 * I'm so lazy....
 * -----by Sun
 */
public class PersistentHttpCookieStore implements CookieStore {

    private static final String PREFS_COOKIE = "CookiePrefsFile";
    private static final String COOKIES_URI = "url";

    /**
     * This map may have null keys!
     */
    @NonNull
    private final Map<URI, List<HttpCookie>> map;

    private final SharedPreferences cookieSP;

    public PersistentHttpCookieStore(@NonNull Context context) {
        map = new HashMap<>();
        cookieSP = context.getSharedPreferences(PREFS_COOKIE, Context.MODE_PRIVATE);

        // get each cookie's URI string
        Set<String> cookiesURL = cookieSP.getStringSet(COOKIES_URI, Collections.emptySet());
        for (String uri : cookiesURL) {
            // get corresponding cookies' key of the shared preference
            Set<String> cookiesName = cookieSP.getStringSet(uri, Collections.emptySet());

            // get corresponding cookies
            List<HttpCookie> httpCookies = new ArrayList<>();
            for (String name : cookiesName) {
                HttpCookie httpCookie = decodeCookie(cookieSP.getString(name, null));
                if (httpCookie != null) {
                    httpCookies.add(httpCookie);
                }
            }

            map.put(URI.create(uri), httpCookies);
        }
    }

    @Override
    public synchronized void add(URI uri, @Nullable HttpCookie httpCookie) {
        if (httpCookie == null) {
            throw new NullPointerException("cookie == null");
        }

        boolean isUriNew = false;
        uri = cookiesUri(uri);
        List<HttpCookie> cookies = map.get(uri);
        if (cookies == null) {
            cookies = new ArrayList<>();
            map.put(uri, cookies);

            isUriNew = true;
        } else {
            cookies.remove(httpCookie);
        }
        cookies.add(httpCookie);

        String uriString = uri.toString();
        SharedPreferences.Editor editor = cookieSP.edit();

        if (isUriNew) {
            // add new cookie's URL string
            // see http://stackoverflow.com/q/14034803
            Set<String> cookiesURL = new HashSet<>(cookieSP.getStringSet(COOKIES_URI,
                    Collections.emptySet()));
            cookiesURL.add(uriString);

            editor.putStringSet(COOKIES_URI, cookiesURL);
        }

        // add corresponding cookies
        Set<String> cookiesName = new HashSet<>(cookieSP.getStringSet(uriString,
                Collections.emptySet()));
        String cookieNameWithUri = uriString + httpCookie.getName();
        cookiesName.add(cookieNameWithUri);

        editor.putStringSet(uriString, cookiesName);
        editor.putString(cookieNameWithUri, encodeCookie(httpCookie));
        editor.apply();
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
        List<URI> result = new ArrayList<>(map.keySet());
        result.remove(null); // sigh
        return Collections.unmodifiableList(result);
    }

    @Override
    public synchronized boolean remove(URI uri, @Nullable HttpCookie httpCookie) {
        if (httpCookie == null) {
            throw new NullPointerException("cookie == null");
        }

        uri = cookiesUri(uri);
        List<HttpCookie> cookies = map.get(uri);
        if (cookies != null) {
            SharedPreferences.Editor editor = cookieSP.edit();
            String uriString = uri.toString();
            Set<String> cookiesName = new HashSet<>(cookieSP.getStringSet(uriString,
                    Collections.emptySet()));
            String cookieNameWithURI = uriString + httpCookie.getName();

            // remove cookie's URI string
            cookiesName.remove(cookieNameWithURI);
            editor.putStringSet(uriString, cookiesName);
            // remove corresponding cookies
            editor.remove(cookieNameWithURI);
            editor.apply();

            return cookies.remove(httpCookie);
        } else {
            return false;
        }
    }

    @Override
    public synchronized boolean removeAll() {
        // clear cookies from shared preference
        SharedPreferences.Editor editor = cookieSP.edit();
        editor.clear();
        editor.apply();

        // clear cookies from local store
        boolean result = !map.isEmpty();
        map.clear();
        return result;
    }

    /**
     * Parcels HttpCookie object into a String.
     */
    private String encodeCookie(@Nullable HttpCookie httpCookie) {
        if (httpCookie == null) {
            return null;
        }

        Parcel parcel = Parcel.obtain();
        new HttpCookieParcelable(httpCookie).writeToParcel(parcel, 0);
        byte[] bytes = parcel.marshall();
        parcel.recycle();

        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    /**
     * Returns HttpCookie from cookie string.
     */
    private HttpCookie decodeCookie(@Nullable String s) {
        if (s == null) {
            return null;
        }

        byte[] bytes = Base64.decode(s, Base64.DEFAULT);

        Parcel parcel = Parcel.obtain();
        parcel.unmarshall(bytes, 0, bytes.length);
        parcel.setDataPosition(0);

        return HttpCookieParcelable.CREATOR.createFromParcel(parcel).httpCookie;
    }

    private static final class HttpCookieParcelable implements Parcelable {

        private static final Creator<HttpCookieParcelable> CREATOR = new Creator<HttpCookieParcelable>() {

            @NonNull
            @Override
            public HttpCookieParcelable createFromParcel(@NonNull Parcel source) {
                return new HttpCookieParcelable(source);
            }

            @Override
            public HttpCookieParcelable[] newArray(int i) {
                return new HttpCookieParcelable[i];
            }
        };

        private final HttpCookie httpCookie;

        private HttpCookieParcelable(HttpCookie httpCookie) {
            this.httpCookie = httpCookie;
        }

        private HttpCookieParcelable(@NonNull Parcel source) {
            String name = source.readString();
            String value = source.readString();
            httpCookie = new HttpCookie(name, value);
            httpCookie.setComment(source.readString());
            httpCookie.setCommentURL(source.readString());
            httpCookie.setDiscard(source.readByte() != 0);
            httpCookie.setDomain(source.readString());
            httpCookie.setMaxAge(source.readLong());
            httpCookie.setPath(source.readString());
            httpCookie.setPortlist(source.readString());
            httpCookie.setSecure(source.readByte() != 0);
            httpCookie.setVersion(source.readInt());
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            dest.writeString(httpCookie.getName());
            dest.writeString(httpCookie.getValue());
            dest.writeString(httpCookie.getComment());
            dest.writeString(httpCookie.getCommentURL());
            dest.writeByte((byte) (httpCookie.getDiscard() ? 1 : 0));
            dest.writeString(httpCookie.getDomain());
            dest.writeLong(httpCookie.getMaxAge());
            dest.writeString(httpCookie.getPath());
            dest.writeString(httpCookie.getPortlist());
            dest.writeByte((byte) (httpCookie.getSecure() ? 1 : 0));
            dest.writeInt(httpCookie.getVersion());
        }
    }
}