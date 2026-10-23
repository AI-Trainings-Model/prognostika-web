package com.prognostika.weatherwithyou;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.webkit.WebViewAssetLoader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private WebView web;
    private GeolocationPermissions.Callback geoCallback;
    private String geoOrigin;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private static final String UPDATES = "https://raw.githubusercontent.com/AI-Trainings-Model/prognostika-automation/main/update.json";

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(16, 42, 68));
        getWindow().setNavigationBarColor(Color.rgb(16, 42, 68));
        web = new WebView(this);
        web.setBackgroundColor(Color.rgb(10, 32, 54));
        setContentView(web);
        WebSettings st = web.getSettings();
        st.setJavaScriptEnabled(true);
        st.setDomStorageEnabled(true);
        st.setGeolocationEnabled(true);
        st.setAllowFileAccess(false);
        st.setAllowContentAccess(false);
        final WebViewAssetLoader loader = new WebViewAssetLoader.Builder().addPathHandler(
            "/assets/", new WebViewAssetLoader.AssetsPathHandler(this)).build();
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                String target = getIntent().getStringExtra("screen");
                if (target != null && (target.equals("editor") || target.equals("settings"))) {
                    view.evaluateJavascript("window.weatherGoTo && window.weatherGoTo('"+target+"')", null);
                }
            }
            @Override public android.webkit.WebResourceResponse shouldInterceptRequest(
                WebView view, android.webkit.WebResourceRequest request) {
                return loader.shouldInterceptRequest(request.getUrl());
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    callback.invoke(origin, true, false);
                } else {
                    geoOrigin=origin; geoCallback=callback;
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION}, 51);
                }
            }
        });
        web.addJavascriptInterface(new Bridge(), "WeatherAndroid");
        web.loadUrl("https://appassets.androidplatform.net/assets/index.html");
    }

    @Override public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code==51 && geoCallback!=null) {
            boolean granted=false;
            for (int r:results) if (r==PackageManager.PERMISSION_GRANTED) granted=true;
            geoCallback.invoke(geoOrigin, granted, false);
            geoCallback=null;
        }
    }
    @Override public void onBackPressed() {
        web.evaluateJavascript("window.weatherBack && window.weatherBack()", null);
    }
    public class Bridge {
        @JavascriptInterface public String readConfig() {
            return getPreferences(0).getString("config", "");
        }
        @JavascriptInterface public void saveConfig(String json) {
            getPreferences(0).edit().putString("config",json).apply();
            ui.post(()->WeatherWidgetProvider.updateAll(MainActivity.this));
        }
        @JavascriptInterface public void cacheWeather(String city, String temp, String summary) {
            getPreferences(0).edit().putString("city",city).putString("temp",temp)
                .putString("summary",summary).apply();
            ui.post(()->WeatherWidgetProvider.updateAll(MainActivity.this));
        }
        @JavascriptInterface public String version() { return "1.0.0-alpha1"; }
        @JavascriptInterface public void openUpdates() { ui.post(()->checkForUpdates()); }
        @JavascriptInterface public void openUrl(String url) {
            if (!url.startsWith("https://")) return;
            ui.post(()->startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))));
        }
    }
    private void checkForUpdates() {
        new Thread(()->{
            String message;
            String downloadUrl=null;
            try {
                HttpURLConnection conn=(HttpURLConnection)new URL(UPDATES).openConnection();
                conn.setConnectTimeout(8000); conn.setReadTimeout(8000);
                conn.setRequestProperty("Accept", "application/json");
                if (conn.getResponseCode()!=200) throw new Exception("Update manifest unavailable");
                BufferedReader in=new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder b=new StringBuilder(); String line;
                while((line=in.readLine())!=null) b.append(line);
                JSONObject manifest=new JSONObject(b.toString());
                int next=manifest.optInt("versionCode", 0);
                String url=manifest.optString("apkUrl","");
                if (next>1 && url.startsWith("https://")) {
                    message="Доступна версия "+manifest.optString("versionName");
                    downloadUrl=url;
                } else message="Установлена актуальная версия 1.0.0-alpha1";
            } catch(Exception e) {message="Не удалось проверить обновления. Попробуйте позже.";}
            final String m=message, u=downloadUrl;
            ui.post(()->{
                AlertDialog.Builder dlg=new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Обновление APK").setMessage(m).setNegativeButton("Закрыть",null);
                if(u!=null) dlg.setPositiveButton("Скачать", (d,w)->
                    startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(u))));
                dlg.show();
            });
        }).start();
    }
}
