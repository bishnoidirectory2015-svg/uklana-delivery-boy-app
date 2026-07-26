package com.uklanafood.delivery;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

public class OrderAlertActivity extends AppCompatActivity {
    private String orderId;
    private TextView messageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Wake the display and show the incoming order above the lock screen.
        // API 27+ uses the supported Activity methods; older versions use window flags.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            );
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_alert);
        orderId = getIntent().getStringExtra("order_id");

        ((TextView) findViewById(R.id.alertTitle))
                .setText(getIntent().getStringExtra("title"));
        messageView = findViewById(R.id.alertMessage);
        messageView.setText(getIntent().getStringExtra("message"));

        findViewById(R.id.acceptOrderButton).setOnClickListener(view -> act(true));
        findViewById(R.id.forwardOrderButton).setOnClickListener(view -> act(false));
    }

    private void act(boolean accept) {
        // Stop the alert immediately when either action is pressed.
        RingtoneHelper.stop();
        NotificationHelper.cancel(this, orderId);

        new Thread(() -> {
            try {
                JSONObject response = ApiClient.post(
                        this,
                        accept ? AppConfig.ACCEPT : AppConfig.FORWARD,
                        new JSONObject().put("order_id", orderId)
                );
                runOnUiThread(() -> {
                    startActivity(new Intent(this, MainActivity.class)
                            .putExtra("message", response.optString("message")));
                    finish();
                });
            } catch (Exception exception) {
                runOnUiThread(() -> messageView.setText(exception.getMessage()));
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        RingtoneHelper.stop();
        super.onDestroy();
    }
}
