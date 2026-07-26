package com.uklanafood.delivery;

import android.content.Context;
import android.provider.Settings;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class ApiClient {
    private ApiClient() {}
    public static String deviceId(Context c) {
        String id = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ANDROID_ID);
        return id == null ? "" : id.trim();
    }
    public static JSONObject get(Context c, String url) throws Exception { return request(c,url,"GET",null); }
    public static JSONObject post(Context c, String url, JSONObject body) throws Exception { return request(c,url,"POST",body); }
    private static JSONObject request(Context ctx,String url,String method,JSONObject body)throws Exception{
        HttpURLConnection conn=(HttpURLConnection)new URL(url).openConnection();
        conn.setRequestMethod(method); conn.setConnectTimeout(15000); conn.setReadTimeout(15000);
        conn.setRequestProperty("Accept","application/json");
        conn.setRequestProperty("X-UKF-Device-ID",deviceId(ctx));
        String token=SessionManager.token(ctx); if(!token.isEmpty()) conn.setRequestProperty("Authorization","Bearer "+token);
        if(body!=null){conn.setDoOutput(true);conn.setRequestProperty("Content-Type","application/json; charset=UTF-8");try(OutputStream out=conn.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}}
        int code=conn.getResponseCode(); String text=read(code>=200&&code<300?conn.getInputStream():conn.getErrorStream());
        JSONObject root=new JSONObject(text.isEmpty()?"{}":text); root.put("_http_code",code); return root;
    }
    private static String read(InputStream in)throws Exception{if(in==null)return "{}";StringBuilder s=new StringBuilder();try(BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8))){String line;while((line=r.readLine())!=null)s.append(line);}return s.toString();}
}
