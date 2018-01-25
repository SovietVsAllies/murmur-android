package org.moandor.securemessage;

import android.app.Application;
import android.os.Handler;

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
