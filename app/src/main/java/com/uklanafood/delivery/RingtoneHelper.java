package com.uklanafood.delivery;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;

public final class RingtoneHelper {
    private static MediaPlayer player;
    private RingtoneHelper() {}

    public static Uri getSelectedUri(Context context) {
        String saved = context.getSharedPreferences("ukf_sound", Context.MODE_PRIVATE)
            .getString("ringtone_uri", "");
        if (saved != null && !saved.isEmpty()) return Uri.parse(saved);
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        return uri;
    }

    public static synchronized void start(Context context) {
        stop();
        try {
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
            player.setDataSource(context, getSelectedUri(context));
            player.setLooping(true);
            player.setVolume(1.0f, 1.0f);
            player.setOnPreparedListener(mp -> mp.start());
            player.prepareAsync();
        } catch (Exception ignored) {
            stop();
        }
    }

    public static synchronized void stop() {
        try {
            if (player != null) {
                if (player.isPlaying()) player.stop();
                player.reset();
                player.release();
            }
        } catch (Exception ignored) {}
        player = null;
    }
}
