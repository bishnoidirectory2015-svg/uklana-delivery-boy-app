package com.uklanafood.delivery;
import android.content.*;
public final class SessionManager {
 private static final String P="ukf_delivery_login"; private SessionManager(){}
 private static SharedPreferences p(Context c){return c.getSharedPreferences(P,Context.MODE_PRIVATE);} 
 public static void save(Context c,String t,String n,String ph,int priority){p(c).edit().putString("token",t).putString("name",n).putString("phone",ph).putInt("priority",priority).apply();}
 public static String token(Context c){return p(c).getString("token","");} public static String name(Context c){return p(c).getString("name","");} public static String phone(Context c){return p(c).getString("phone","");} public static int priority(Context c){return p(c).getInt("priority",0);} public static boolean loggedIn(Context c){return !token(c).isEmpty();} public static void clear(Context c){p(c).edit().clear().apply();}
 public static void saveFcmToken(Context c,String t){p(c).edit().putString("fcm",t).apply();}
}
