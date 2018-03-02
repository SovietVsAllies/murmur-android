package org.moandor.securemessage;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.AsyncTask;
import android.os.Bundle;

import org.moandor.securemessage.networking.PeerNetwork;

import java.io.IOException;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentManager manager = getFragmentManager();
        Fragment fragment = manager.findFragmentById(android.R.id.content);
        if (fragment == null) {
            fragment = new MainFragment();
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.add(android.R.id.content, fragment);
            transaction.commit();
        }
        AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    new PeerNetwork();
                } catch (IOException | IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
