package org.moandor.securemessage.utils;

import android.support.annotation.NonNull;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class HttpParams {
    @NonNull
    private Map<String, String> mParams = new HashMap<String, String>();

    public void put(String key, String value) {
        mParams.put(key, value);
    }

    public void put(String key, int value) {
        mParams.put(key, String.valueOf(value));
    }

    public void put(String key, long value) {
        mParams.put(key, String.valueOf(value));
    }

    public void put(String key, double value) {
        mParams.put(key, String.valueOf(value));
    }

    public void clear() {
        mParams.clear();
    }

    public boolean empty() {
        return mParams.isEmpty();
    }

    public Set<String> keySet() {
        return mParams.keySet();
    }

    public String encodeUrl() {
        StringBuilder stringBuilder = new StringBuilder();
        boolean first = true;
        for (String key : mParams.keySet()) {
            String value = mParams.get(key);
            if (!TextUtils.isEmpty(value)) {
                if (first) {
                    first = false;
                } else {
                    stringBuilder.append("&");
                }
                stringBuilder.append(encodeUrl(key)).append("=").append(encodeUrl(value));
            }
        }
        return stringBuilder.toString();
    }

    public String getParam(String key) {
        return mParams.get(key);
    }

    private static String encodeUrl(String string) {
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
