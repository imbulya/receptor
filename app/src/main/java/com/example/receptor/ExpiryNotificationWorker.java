package com.example.receptor;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class ExpiryNotificationWorker extends Worker {

    public static final String CHANNEL_ID = "expiry_alerts";
    private static final int NOTIFICATION_ID = 1001;
    private static final String WORK_NAME = "expiry_notifications";

    public ExpiryNotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    public static void schedule(Context context) {
        if (context == null) {
            return;
        }
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                ExpiryNotificationWorker.class,
                12,
                TimeUnit.HOURS
        ).build();

        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        AppRepository repository = AppRepository.getInstance(context);
        int warningDays = UserPreferences.getExpiryWarningDays(context, repository.getExpiryWarningDays());
        repository.setExpiryWarningDays(warningDays);

        List<AppRepository.Product> products = repository.getAllProducts();
        int expiredCount = 0;
        int expiringCount = 0;

        for (AppRepository.Product product : products) {
            if (product.units.isEmpty()) {
                continue;
            }

            boolean hasExpired = false;
            boolean hasExpiring = false;
            for (AppRepository.ProductUnit unit : product.units) {
                AppRepository.ExpiryStatus status = repository.getExpiryStatus(unit.expiryDate);
                if (status == AppRepository.ExpiryStatus.EXPIRED) {
                    hasExpired = true;
                } else if (status == AppRepository.ExpiryStatus.EXPIRING) {
                    hasExpiring = true;
                }
            }

            if (hasExpired) {
                expiredCount++;
            } else if (hasExpiring) {
                expiringCount++;
            }
        }

        if (expiredCount + expiringCount == 0) {
            return Result.success();
        }

        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            return Result.success();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return Result.success();
        }

        createChannelIfNeeded(context);

        String content = buildContentText(context, expiredCount, expiringCount);
        PendingIntent intent = PendingIntent.getActivity(
                context,
                0,
                new Intent(context, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(context.getString(R.string.notifications_title))
                .setContentText(content)
                .setAutoCancel(true)
                .setContentIntent(intent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
        return Result.success();
    }

    private void createChannelIfNeeded(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notifications_channel),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription(context.getString(R.string.notifications_channel_desc));
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private String buildContentText(Context context, int expiredCount, int expiringCount) {
        if (expiredCount > 0 && expiringCount > 0) {
            return context.getString(R.string.notifications_both, expiredCount, expiringCount);
        }
        if (expiredCount > 0) {
            return context.getString(R.string.notifications_expired_only, expiredCount);
        }
        return context.getString(R.string.notifications_expiring_only, expiringCount);
    }
}
