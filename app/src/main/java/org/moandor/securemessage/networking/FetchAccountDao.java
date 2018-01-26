package org.moandor.securemessage.networking;

import org.json.JSONException;
import org.moandor.securemessage.R;
import org.moandor.securemessage.models.Account;
import org.moandor.securemessage.utils.HttpUtils;
import org.moandor.securemessage.utils.NotifyException;
import org.moandor.securemessage.utils.UrlHelper;

import java.util.UUID;

public class FetchAccountDao implements BaseDao<Account> {
    private UUID mAccountId;

    public FetchAccountDao(UUID accountId) {
        mAccountId = accountId;
    }

    @Override
    public Account execute() throws NotifyException {
        String response = HttpUtils.doGet(UrlHelper.API_ACCOUNTS + mAccountId + '/');
        try {
            return Account.parseJson(response);
        } catch (JSONException e) {
            e.printStackTrace();
            throw new NotifyException(R.string.json_error);
        }
    }
}
