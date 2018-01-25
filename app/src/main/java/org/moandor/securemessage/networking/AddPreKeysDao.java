package org.moandor.securemessage.networking;

import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.moandor.securemessage.GlobalContext;
import org.moandor.securemessage.R;
import org.moandor.securemessage.utils.HttpParams;
import org.moandor.securemessage.utils.HttpUtils;
import org.moandor.securemessage.utils.NotifyException;
import org.moandor.securemessage.utils.UrlHelper;

import java.util.List;
import java.util.UUID;

public class AddPreKeysDao implements BaseDao<Void> {
    private final UUID mAccount;
    private final List<Integer> mKeyIds;
    private final List<byte[]> mKeys;

    public AddPreKeysDao(UUID account, List<Integer> keyIds, List<byte[]> keys) {
        mAccount = account;
        mKeyIds = keyIds;
        mKeys = keys;
    }

    @Override
    public Void execute() throws NotifyException {
        HttpParams params = new HttpParams();
        params.put("account", mAccount.toString());
        JSONObject data = new JSONObject();
        try {
            data.put("account", mAccount.toString());
            data.put("key_ids", new JSONArray(mKeyIds));
            JSONArray keys = new JSONArray();
            for (byte[] key : mKeys) {
                keys.put(Base64.encodeToString(key, Base64.DEFAULT));
            }
            data.put("keys", keys);
            HttpUtils.doPost(UrlHelper.API_PRE_KEYS, data.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            throw new NotifyException(GlobalContext.getInstance().getString(R.string.json_error));
        }
        return null;
    }
}
