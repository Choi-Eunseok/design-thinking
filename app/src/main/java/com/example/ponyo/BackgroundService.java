package com.example.ponyo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

public class BackgroundService extends Service {
    private final static String TAG = "Notification";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel notificationChannel =
                    new NotificationChannel(
                            "channel_id",
                            "test",
                            NotificationManager.IMPORTANCE_DEFAULT
                    );
            notificationChannel.setDescription("notification");
            notificationManager.createNotificationChannel(notificationChannel);


        }
        //else
        //startForeground(1, new Notification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        super.onStartCommand(intent, flags, startId);
        //Notification notification1 = new Notification(R.drawable.ic_launcher_foreground, "서비스 실행됨", System.currentTimeMillis());
        //notification.setLatestEventInfo(getApplicationContext(), "Screen Service", "Foreground로 실행됨", null);
        //startForeground(1, notification1);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "channel_id");
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.pngegg)
                .setContentTitle("PONYO is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }
}