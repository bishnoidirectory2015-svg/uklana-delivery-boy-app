package com.uklanafood.delivery;
import android.content.*;import android.os.*;import android.view.*;import android.widget.*;import androidx.appcompat.app.AppCompatActivity;import org.json.*;
public class LoginActivity extends AppCompatActivity{
 EditText phone,pin; TextView msg; Button login;
 protected void onCreate(Bundle b){super.onCreate(b);if(SessionManager.loggedIn(this)){open();return;}setContentView(R.layout.activity_login);phone=findViewById(R.id.loginPhone);pin=findViewById(R.id.loginPin);msg=findViewById(R.id.loginMessage);login=findViewById(R.id.loginButton);login.setOnClickListener(v->go());}
 void go(){String p=phone.getText().toString().replaceAll("\\D+","");String x=pin.getText().toString().trim();login.setEnabled(false);new Thread(()->{try{JSONObject q=new JSONObject();q.put("phone",p);q.put("pin",x);q.put("device_id",ApiClient.deviceId(this));JSONObject r=ApiClient.post(this,AppConfig.LOGIN,q);if(!r.optBoolean("ok"))throw new Exception(r.optString("message","Login failed"));JSONObject d=r.getJSONObject("delivery_boy");SessionManager.save(this,r.getString("token"),d.getString("name"),d.getString("phone"),d.getInt("priority"));runOnUiThread(this::open);}catch(Exception e){runOnUiThread(()->{login.setEnabled(true);msg.setText(e.getMessage());});}}).start();}
 void open(){startActivity(new Intent(this,MainActivity.class));finish();}
}
