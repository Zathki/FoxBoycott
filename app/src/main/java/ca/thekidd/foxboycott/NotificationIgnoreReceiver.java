package ca.thekidd.foxboycott;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class NotificationIgnoreReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        String placeId = intent.getStringExtra("placeID");
        int notificationId = intent.getIntExtra("notificationID", 0);
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(notificationId);

        if(placeId != null) {
            SharedPreferences prefs = context.getSharedPreferences("ignore", Context.MODE_PRIVATE);
            prefs.edit().putBoolean(placeId, true).commit();
        }
    };
}