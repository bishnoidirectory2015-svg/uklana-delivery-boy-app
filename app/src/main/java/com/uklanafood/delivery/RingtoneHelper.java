package com.uklanafood.delivery;

import android.content.Context;
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
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        return uri;
    }

    public static synchronized void start(Context context) {
        stop();
        try {
            player = MediaPlayer.create(context, getSelectedUri(context));
            if (player != null) { player.setLooping(true); player.start(); }
        } catch (Exception ignored) {}
    }

    public static synchronized void stop() {
        try {
            if (player != null) {
                if (player.isPlaying()) player.stop();
                player.release();
            }
        } catch (Exception ignored) {}
        player = null;
    }
}
