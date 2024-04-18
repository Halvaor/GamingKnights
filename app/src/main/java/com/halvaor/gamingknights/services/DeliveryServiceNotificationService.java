package com.halvaor.gamingknights.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.halvaor.gamingknights.R;
import com.halvaor.gamingknights.activities.GameNightActivity;

import java.util.concurrent.atomic.AtomicInteger;

public class DeliveryServiceNotificationService extends Service {

    private AtomicInteger notificationID = new AtomicInteger();
    private String channelID = "1";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        buildNotification();
    }

    public void buildNotification() {
        String notificationText = "Deinem ausstehenden Spieleabend wurde ein Essenslieferant hinzugefügt. \n" +
                "Bitte hinterlege deine Bestellung.";

        Intent intent = new Intent(DeliveryServiceNotificationService.this, GameNightActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("gameNightID", "Test");

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelID)
                .setSmallIcon(R.drawable.settings_icon_white)
                .setContentTitle("Essen auswählen")
                .setContentText(notificationText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = createNotificationChannel();
        // notificationId is a unique int for each notification that you must define.

        if(notificationManager != null) {
            notificationManager.notify(this.notificationID.incrementAndGet(), builder.build());
        }
    }

    private NotificationManager createNotificationChannel() {
        NotificationManager notificationManager = null;
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is not in the Support Library.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(channelID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this.

            notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        return notificationManager;
    }


}
