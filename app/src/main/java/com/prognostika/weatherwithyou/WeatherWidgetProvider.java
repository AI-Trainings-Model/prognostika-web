package com.prognostika.weatherwithyou;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class WeatherWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_REFRESH="com.prognostika.weatherwithyou.REFRESH_WIDGET";
    private static final String[] DEFAULT_MODULES={"hero","hourly","daily","details","family","clothes","warnings","radar"};

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for(int id:ids) render(context,manager,id);
    }

    @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle options){
        super.onAppWidgetOptionsChanged(c,m,id,options);
        render(c,m,id);
    }

    @Override public void onReceive(Context c,Intent intent){
        super.onReceive(c,intent);
        if(ACTION_REFRESH.equals(intent.getAction())){
            final PendingResult pending=goAsync();
            new Thread(()->{try{fetchWeather(c);}finally{updateAll(c);pending.finish();}}).start();
        }
    }

    public static void updateAll(Context context){
        AppWidgetManager m=AppWidgetManager.getInstance(context);
        for(int id:m.getAppWidgetIds(new ComponentName(context,WeatherWidgetProvider.class))){
            render(context,m,id);
        }
    }

    private static PendingIntent mainIntent(Context c,String screen,int requestCode){
        Intent open=new Intent(c,MainActivity.class).putExtra("screen",screen)
          .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(c,requestCode,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }

    private static String icons(JSONArray family){
        StringBuilder s=new StringBuilder();
        if(family!=null)for(int i=0;i<family.length()&&i<7;i++){
            switch(family.optString(i)){
                case "male":s.append("👨");break;
                case "female":s.append("👩");break;
                case "child":s.append("🧒");break;
                case "dog":s.append("🐕");break;
                case "cat":s.append("🐈");break;
                default:break;
            }
        }
        return s.length()>0?s.toString():"👤";
    }

    private static String locale(Context c, JSONObject cfg) {
        String choice=cfg.optString("language","system");
        String language="system".equals(choice)?java.util.Locale.getDefault().getLanguage():choice;
        return language.matches("ru|en|de|pl|be")?language:"en";
    }
    // Stable internal module IDs and phrase keys, separate from translated UI strings.
    private static String tr(String lang, String key) {
        final String[][] terms={
            {"hourly","Почасовой прогноз","Hourly forecast","Stündliche Vorhersage","Prognoza godzinowa","Пагадзінны прагноз"},
            {"daily","Прогноз на 7 дней","7-day forecast","7-Tage-Vorhersage","Prognoza na 7 dni","Прагноз на 7 дзён"},
            {"details","Подробные показатели","Weather details","Wetterdetails","Szczegóły pogody","Падрабязныя паказчыкі"},
            {"clothes","Что надеть","What to wear","Was anziehen?","Co ubrać","Што апрануць"},
            {"family","Вся семья","Whole family","Ganze Familie","Cała rodzina","Уся сям’я"},
            {"warnings","Предупреждения","Advisories","Warnhinweise","Ostrzeżenia","Папярэджанні"},
            {"radar","Карта осадков","Rain map","Niederschlagskarte","Mapa opadów","Карта ападкаў"},
            {"hint","Настройте модули","Configure modules","Module einrichten","Skonfiguruj moduły","Наладзьце модулі"},
            {"openHours","Нажмите, чтобы открыть часы","Tap for hours","Für Stunden tippen","Dotknij, aby zobaczyć godziny","Націсніце, каб адкрыць гадзіны"},
            {"dailyText","Температура и осадки по дням","Daily temperatures and rain","Temperatur und Regen nach Tagen","Temperatura i opady w dniach","Тэмпература і ападкі па днях"},
            {"windText","Ветер · влажность · давление","Wind · humidity · pressure","Wind · Feuchte · Luftdruck","Wiatr · wilgotność · ciśnienie","Вецер · вільготнасць · ціск"},
            {"humidityText"," влажность"," humidity"," Feuchte"," wilgotność"," вільготнасць"},
            {"walk","Рекомендации для прогулки","Walking recommendations","Empfehlungen für Spaziergänge","Zalecenia na spacer","Парады для прагулкі"},
            {"changes","Прогноз изменений погоды","Weather changes","Wetteränderungen","Zmiany pogody","Змены надвор’я"},
            {"radarPlaceholder","Здесь будет радар","Radar coming soon","Radar folgt","Radar wkrótce","Радар хутка"},
            {"openEditor","Откройте редактор","Open editor","Editor öffnen","Otwórz edytor","Адкрыйце рэдактар"},
            {"loading","Прогноз загружается","Loading forecast","Vorhersage wird geladen","Ładowanie prognozy","Прагноз загружаецца"},
            {"clear","Ясно","Clear","Klar","Bezchmurnie","Ясна"},
            {"partly","Переменная облачность","Partly cloudy","Wechselnd bewölkt","Częściowe zachmurzenie","Пераменная воблачнасць"},
            {"cloudy","Облачно","Cloudy","Bewölkt","Pochmurno","Воблачна"},
            {"snow","Снег","Snow","Schnee","Śnieg","Снег"},
            {"rain","Дождь","Rain","Regen","Deszcz","Дождж"},
            {"fog","Туман","Fog","Nebel","Mgła","Туман"},
            {"berlin","Берлин","Berlin","Berlin","Berlin","Берлін"}
        };
        int index="ru".equals(lang)?1:"en".equals(lang)?2:"de".equals(lang)?3:"pl".equals(lang)?4:5;
        for(String[] phrase:terms)if(phrase[0].equals(key))return phrase[index];
        return key;
    }
    private static String translateCondition(String lang,String value){
        if(value.equals("Ясно"))return tr(lang,"clear");
        if(value.equals("Переменная облачность"))return tr(lang,"partly");
        if(value.equals("Облачно"))return tr(lang,"cloudy");
        if(value.equals("Снег"))return tr(lang,"snow");
        if(value.equals("Дождь"))return tr(lang,"rain");
        if(value.equals("Туман"))return tr(lang,"fog");
        return value;
    }
    private static void render(Context c,AppWidgetManager manager,int id){
        SharedPreferences p=c.getSharedPreferences("MainActivity",Context.MODE_PRIVATE);
        JSONObject cfg;
        try{cfg=new JSONObject(p.getString("config","{}"));}catch(Exception e){cfg=new JSONObject();}
        RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.weather_widget);
        String lang=locale(c,cfg);
        String city=p.getString("city",cfg.optString("city",tr(lang,"berlin")));
        String temp=p.getString("temp","—°");
        String summary=translateCondition(lang,p.getString("summary",tr(lang,"loading")));
        v.setTextViewText(R.id.widget_city,city);
        v.setTextViewText(R.id.widget_temp,temp);
        v.setTextViewText(R.id.widget_summary,summary);
        v.setTextViewText(R.id.widget_people,icons(cfg.optJSONArray("family")));
        v.setOnClickPendingIntent(R.id.widget_root,mainIntent(c,"home",id*10+1));
        v.setOnClickPendingIntent(R.id.widget_edit,mainIntent(c,"editor",id*10+2));
        Intent refresh=new Intent(c,WeatherWidgetProvider.class).setAction(ACTION_REFRESH);
        PendingIntent retry=PendingIntent.getBroadcast(c,id,refresh,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        v.setOnClickPendingIntent(R.id.widget_refresh,retry);

        // RemoteViews' module container is rebuilt on every configuration change.
        v.removeAllViews(R.id.widget_modules);
        JSONArray mods=cfg.optJSONArray("modules");
        boolean heroEnabled=mods==null;
        if(mods!=null)for(int i=0;i<mods.length();i++){
            JSONObject item=mods.optJSONObject(i);
            if(item!=null&&"hero".equals(item.optString("id")))heroEnabled=item.optBoolean("enabled",true);
        }
        v.setViewVisibility(R.id.widget_hero,heroEnabled?android.view.View.VISIBLE:android.view.View.GONE);
        int count=0;
        if(mods!=null){
            for(int i=0;i<mods.length()&&count<4;i++){
                JSONObject item=mods.optJSONObject(i);
                if(item==null||!item.optBoolean("enabled",true))continue;
                String kind=item.optString("id","");
                if("hero".equals(kind))continue;
                append(c,v,kind,temp,summary,icons(cfg.optJSONArray("family")),p,id*100+i,count,lang);
                count++;
            }
        }else{
            for(String kind:new String[]{"hourly","daily","details"}){
                append(c,v,kind,temp,summary,icons(cfg.optJSONArray("family")),p,id*100+count,count,lang);
                count++;
            }
        }
        if(count==0 && !heroEnabled){
            append(c,v,"hint",temp,summary,"",p,id*100,0,lang);
        }
        manager.updateAppWidget(id,v);
    }

    private static void append(Context c,RemoteViews container,String id,String temp,String description,String people,SharedPreferences p,int requestCode,int index,String lang){
        String title,icon,value;
        switch(id){
            case "hourly":title=tr(lang,"hourly");icon="◷";value=tr(lang,"openHours");break;
            case "daily":title=tr(lang,"daily");icon="▦";value=tr(lang,"dailyText");break;
            case "details":title=tr(lang,"details");icon="◉";value=p.getString("humidity","")+
                (p.getString("humidity","").isEmpty()?tr(lang,"windText"):tr(lang,"humidityText"));break;
            case "clothes":title=tr(lang,"clothes");icon="♧";value=tr(lang,"walk");break;
            case "family":title=tr(lang,"family");icon="♧";value=people;break;
            case "warnings":title=tr(lang,"warnings");icon="⚠";value=tr(lang,"changes");break;
            case "radar":title=tr(lang,"radar");icon="◈";value=tr(lang,"radarPlaceholder");break;
            default:title=tr(lang,"hint");icon="✎";value=tr(lang,"openEditor");break;
        }
        RemoteViews row=new RemoteViews(c.getPackageName(),R.layout.widget_module);
        row.setTextViewText(R.id.widget_module_icon,icon);
        row.setTextViewText(R.id.widget_module_title,title);
        row.setTextViewText(R.id.widget_module_value,value);
        row.setOnClickPendingIntent(R.id.widget_module_click,mainIntent(c,
            id.equals("hint")?"editor":"home",requestCode));
        container.addView(R.id.widget_modules,row);
    }

    private static void fetchWeather(Context c){
        HttpURLConnection conn=null;
        try{
            SharedPreferences p=c.getSharedPreferences("MainActivity",Context.MODE_PRIVATE);
            JSONObject cfg=new JSONObject(p.getString("config","{}"));
            double lat=cfg.optDouble("lat",52.52);
            double lon=cfg.optDouble("lon",13.405);
            String link="https://api.open-meteo.com/v1/forecast?latitude="+lat+
              "&longitude="+lon+"&current=temperature_2m,relative_humidity_2m,weather_code";
            conn=(HttpURLConnection)new URL(link).openConnection();
            conn.setConnectTimeout(8000);conn.setReadTimeout(8000);
            if(conn.getResponseCode()!=200)return;
            InputStream stream=conn.getInputStream();ByteArrayOutputStream b=new ByteArrayOutputStream();
            byte[] buffer=new byte[4096];int n;
            while((n=stream.read(buffer))!=-1)b.write(buffer,0,n);
            JSONObject cur=new JSONObject(new String(b.toByteArray(),StandardCharsets.UTF_8)).getJSONObject("current");
            int value=(int)Math.round(cur.getDouble("temperature_2m"));
            int code=cur.optInt("weather_code",2);
            String description=code==0?"Ясно":code<=2?"Переменная облачность":code==3?"Облачно":code>=71&&code<90?"Снег":code>=51?"Дождь":"Туман";
            p.edit().putString("temp",value+"°").putString("summary",description)
                .putString("humidity",cur.optInt("relative_humidity_2m",0)+"%")
                .putLong("widget_updated_at",System.currentTimeMillis()).apply();
        }catch(Exception ignored){}finally{if(conn!=null)conn.disconnect();}
    }
}
