package com.uklanafood.delivery;
import android.app.*;import android.content.*;import android.os.*;import android.view.*;import android.widget.*;import androidx.appcompat.app.AppCompatActivity;import org.json.*;
public class OrderAlertActivity extends AppCompatActivity{
 String oid; TextView msg;
 protected void onCreate(Bundle b){super.onCreate(b);if(Build.VERSION.SDK_INT>=27)setShowWhenLocked(true);setTurnScreenOn(true);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);setContentView(R.layout.activity_alert);oid=getIntent().getStringExtra("order_id");((TextView)findViewById(R.id.alertTitle)).setText(getIntent().getStringExtra("title"));msg=findViewById(R.id.alertMessage);msg.setText(getIntent().getStringExtra("message"));findViewById(R.id.acceptOrderButton).setOnClickListener(v->act(true));findViewById(R.id.forwardOrderButton).setOnClickListener(v->act(false));}
 void act(boolean accept){NotificationHelper.cancel(this,oid);new Thread(()->{try{JSONObject r=ApiClient.post(this,accept?AppConfig.ACCEPT:AppConfig.FORWARD,new JSONObject().put("order_id",oid));runOnUiThread(()->{startActivity(new Intent(this,MainActivity.class).putExtra("message",r.optString("message")));finish();});}catch(Exception e){runOnUiThread(()->msg.setText(e.getMessage()));}}).start();}
 protected void onDestroy(){super.onDestroy();RingtoneHelper.stop();}
}
