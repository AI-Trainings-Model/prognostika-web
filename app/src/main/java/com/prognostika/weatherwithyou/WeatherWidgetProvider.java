package com.prognostika.weatherwithyou;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import org.json.JSONObject;

public class WeatherWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        for(int id:ids) update(c,m,id);
    }
    public static void updateAll(Context c) {
        AppWidgetManager manager=AppWidgetManager.getInstance(c);
        ComponentName comp=new ComponentName(c,WeatherWidgetProvider.class);
        for(int id:manager.getAppWidgetIds(comp)) update(c,manager,id);
    }
    private static void update(Context c,AppWidgetManager manager,int id) {
        RemoteViews view=new RemoteViews(c.getPackageName(),R.layout.weather_widget);
        android.content.SharedPreferences p=c.getSharedPreferences("MainActivity",Context.MODE_PRIVATE);
        view.setTextViewText(R.id.widget_city,p.getString("city","Берлин"));
        view.setTextViewText(R.id.widget_temp,p.getString("temp","12°"));
        view.setTextViewText(R.id.widget_summary,p.getString("summary","Переменная облачность"));
        String emoji="👤";
        try {
            JSONObject config=new JSONObject(p.getString("config","{}"));
            org.json.JSONArray people=config.optJSONArray("family");
            if(people!=null) {
                emoji="";
                for(int i=0;i<people.length()&&i<5;i++) {
                    String kind=people.optString(i,"");
                    emoji+=kind.equals("male")?"👨":kind.equals("female")?"👩":kind.equals("child")?"🧒":kind.equals("dog")?"🐕":"🐈";
                }
            }
        } catch(Exception ignored) {}
        view.setTextViewText(R.id.widget_people,emoji);
        Intent open=new Intent(c,MainActivity.class);
        PendingIntent pending=PendingIntent.getActivity(c,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        view.setOnClickPendingIntent(R.id.widget_root,pending);
        manager.updateAppWidget(id,view);
    }
}
