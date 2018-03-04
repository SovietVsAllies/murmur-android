package org.moandor.securemessage;

import android.app.Application;
import android.content.Intent;
import android.os.Handler;

import org.moandor.securemessage.services.MessageService;

public class GlobalContext extends Application {
    private Handler mHandler;

    private static GlobalContext sInstance;

    @Override
    public void onCreate() {
        sInstance = this;
        super.onCreate();
        mHandler = new Handler();
    }

    public void runOnMainThread(Runnable runnable) {
        mHandler.post(runnable);
    }

    public static GlobalContext getInstance() {
        return sInstance;
    }
}
