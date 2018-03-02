package org.moandor.securemessage.utils;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import org.moandor.securemessage.GlobalContext;
import org.whispersystems.libsignal.util.guava.Optional;

import java.util.UUID;

public class PreferenceUtils {
    public static String getIdentityPrivate() {
        return getPreferences().getString(PREF_IDENTITY_PRIVATE, null);
    }

    public static void setIdentityPrivate(String value) {
        setPreference(PREF_IDENTITY_PRIVATE, value);
    }

    public static String getIdentityPublic() {
        return getPreferences().getString(PREF_IDENTITY_PUBLIC, null);
    }

    public static void setIdentityPublic(String value) {
        setPreference(PREF_IDENTITY_PUBLIC, value);
    }

    public static int getLocalRegistrationId() {
        return getPreferences().getInt(PREF_LOCAL_REG_ID, 0);
    }

    public static void setLocalRegistrationId(int id) {
        setPreference(PREF_LOCAL_REG_ID, id);
    }

    public static Optional<UUID> getLocalAccountId() {
        String account = getPreferences().getString(PREF_LOCAL_ACCOUNT, null);
        if (account == null) {
            return Optional.absent();
        }
        return Optional.of(UUID.fromString(account));
    }

    public static void setLocalAccountId(@NonNull UUID account) {
        setPreference(PREF_LOCAL_ACCOUNT, account.toString());
    }

    public static Optional<UUID> getTargetId() {
        String id = getPreferences().getString(PREF_TARGET_ID, null);
        if (id == null) {
            return Optional.absent();
        }
        return Optional.of(UUID.fromString(id));
    }

    public static void setTargetId(@NonNull UUID id) {
        setPreference(PREF_TARGET_ID, id.toString());
    }

    private static final String PREF_IDENTITY_PRIVATE = "pref-identity-private";
    private static final String PREF_IDENTITY_PUBLIC = "pref-identity-public";
    private static final String PREF_LOCAL_REG_ID = "pref-local-reg-id";
    private static final String PREF_LOCAL_ACCOUNT = "pref-local-account-id";
    private static final String PREF_TARGET_ID = "pref-target-id";

    private static void setPreference(String key, String value) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putString(key, value);
        editor.apply();
    }

    private static void setPreference(String key, int value) {
        SharedPreferences.Editor editor = getPreferences().edit();
        editor.putInt(key, value);
        editor.apply();
    }

    private static SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(GlobalContext.getInstance());
    }
}
