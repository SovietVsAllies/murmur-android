package org.moandor.securemessage;

import android.util.Log;
import android.widget.Toast;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class SecureMessageFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d("message", remoteMessage.getData().toString());
        final RemoteMessage.Notification notification = remoteMessage.getNotification();
        if (notification != null) {
            GlobalContext.getInstance().runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(
                            getApplicationContext(),
                            notification.getBody(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
