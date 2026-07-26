package com.uklanafood.delivery;

import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class RingtoneSettingsActivity extends AppCompatActivity {
    private static final int PICK_RINGTONE = 41;
    private TextView selected;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ringtone_settings);
        selected = findViewById(R.id.txtSelectedRingtone);
        updateName();
        findViewById(R.id.btnChooseRingtone).setOnClickListener(v -> choose());
        findViewById(R.id.btnTestRingtone).setOnClickListener(v -> RingtoneHelper.start(this));
        findViewById(R.id.btnDefaultRingtone).setOnClickListener(v -> {
            RingtoneHelper.stop();
            getSharedPreferences("ukf_sound", MODE_PRIVATE).edit().remove("ringtone_uri").apply();
            updateName();
        });
        findViewById(R.id.btnNotificationSettings).setOnClickListener(v -> {
            Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(i);
        });
    }

    private void choose() {
        Uri current = RingtoneHelper.getSelectedUri(this);
        Intent i = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_RINGTONE);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select New Order Ringtone");
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, current);
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);
        startActivityForResult(i, PICK_RINGTONE);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_RINGTONE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (uri != null) {
                getSharedPreferences("ukf_sound", MODE_PRIVATE).edit().putString("ringtone_uri", uri.toString()).apply();
                updateName();
                RingtoneHelper.start(this);
            }
        }
    }

    private void updateName() {
        Uri uri = RingtoneHelper.getSelectedUri(this);
        Ringtone r = RingtoneManager.getRingtone(this, uri);
        selected.setText(r == null ? "Default Phone Ringtone" : r.getTitle(this));
    }

    @Override protected void onStop() { super.onStop(); RingtoneHelper.stop(); }
}
