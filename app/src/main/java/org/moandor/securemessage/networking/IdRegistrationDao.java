package org.moandor.securemessage.networking;

import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;
import org.moandor.securemessage.GlobalContext;
import org.moandor.securemessage.R;
import org.moandor.securemessage.models.Account;
import org.moandor.securemessage.utils.HttpUtils;
import org.moandor.securemessage.utils.NotifyException;
import org.moandor.securemessage.utils.UrlHelper;

import java.util.UUID;

public class IdRegistrationDao implements BaseDao<Account> {
    private final byte[] mIdentityKey;
    private final byte[] mSignedPreKey;

    public IdRegistrationDao(byte[] identityKey, byte[] signedPreKey) {
        mIdentityKey = identityKey;
        mSignedPreKey = signedPreKey;
    }

    @Override
    public Account execute() throws NotifyException {
        try {
            JSONObject data = new JSONObject();
            data.put("identity_key", Base64.encodeToString(mIdentityKey, Base64.DEFAULT));
            data.put("signed_pre_key", Base64.encodeToString(mSignedPreKey, Base64.DEFAULT));
            String response = HttpUtils.doPost(UrlHelper.API_ACCOUNTS, data.toString());
            return parseJson(response);
        } catch (JSONException e) {
            e.printStackTrace();
            throw new NotifyException(GlobalContext.getInstance().getString(R.string.json_error));
        }
    }

    private Account parseJson(String jsonStr) throws JSONException {
        JSONObject json = new JSONObject(jsonStr);
        return new Account(
                UUID.fromString(json.getString("id")),
                Base64.decode(json.getString("identity_key"), Base64.DEFAULT),
                Base64.decode(json.getString("signed_pre_key"), Base64.DEFAULT));
    }
}
