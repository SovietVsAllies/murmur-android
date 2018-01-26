package org.moandor.securemessage.networking;

import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;
import org.moandor.securemessage.R;
import org.moandor.securemessage.utils.HttpUtils;
import org.moandor.securemessage.utils.NotifyException;
import org.moandor.securemessage.utils.UrlHelper;
import org.whispersystems.libsignal.state.PreKeyRecord;

import java.io.IOException;
import java.util.UUID;

public class FetchPreKeyDao implements BaseDao<PreKeyRecord> {
    private UUID mAccountId;

    public FetchPreKeyDao(UUID accountId) {
        mAccountId = accountId;
    }

    @Override
    public PreKeyRecord execute() throws NotifyException {
        String response = HttpUtils.doGet(
                UrlHelper.API_PRE_KEYS + mAccountId + '/', null);
        try {
            JSONObject json = new JSONObject(response);
            return new PreKeyRecord(Base64.decode(json.getString("key"), Base64.DEFAULT));
        } catch (JSONException e) {
            e.printStackTrace();
            throw new NotifyException(R.string.json_error);
        } catch (IOException e) {
            e.printStackTrace();
            throw new NotifyException(R.string.unknown_error);
        }
    }
}
