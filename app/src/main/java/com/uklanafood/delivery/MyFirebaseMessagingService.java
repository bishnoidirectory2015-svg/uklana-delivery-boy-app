package com.uklanafood.delivery;
import androidx.annotation.NonNull;import com.google.firebase.messaging.*;import org.json.*;import java.util.*;
public class MyFirebaseMessagingService extends FirebaseMessagingService{
 public void onNewToken(@NonNull String t){SessionManager.saveFcmToken(this,t);if(SessionManager.loggedIn(this))new Thread(()->{try{ApiClient.post(this,AppConfig.FCM,new JSONObject().put("fcm_token",t));}catch(Exception ignored){}}).start();}
 public void onMessageReceived(@NonNull RemoteMessage m){Map<String,String>d=m.getData();if(!"delivery_offer".equals(d.get("type")))return;NotificationHelper.show(this,d.get("order_id"),d.get("title"),d.get("body"));}
}
