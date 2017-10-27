package me.sunzheng.mana.utils;

import android.content.Context;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import okio.Sink;

/**
 * Created by Sun on 2017/10/27.
 */

public class CachFileUtil {
    File mFile;

    public CachFileUtil(Context mContext, String fileName) {
        mFile = new File(mContext.getCacheDir(), fileName);
        try {
            if (!mFile.exists())
                mFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public CachFileUtil insert(Object o) {
        try {
            Sink sink = Okio.sink(mFile);
            BufferedSink bufferedSource = Okio.buffer(sink);
            bufferedSource.writeUtf8(new Gson().toJson(o));
            bufferedSource.close();
        } catch (FileNotFoundException e) {

        } catch (IOException e) {

        }
        return this;
    }

    public CachFileUtil delete() {
        if (mFile.exists())
            mFile.delete();
        return this;
    }

    public CachFileUtil update(Object o) {
        return insert(o);
    }

    public <T> T query(Class<T> clz) {
        try {
            okio.Source source = Okio.source(mFile);
            BufferedSource bufferedSource = Okio.buffer(source);
            String s = bufferedSource.readUtf8();
            bufferedSource.close();
            if (!TextUtils.isEmpty(s)) {
                return new Gson().fromJson(s, clz);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }
}
