package com.uklanafood.delivery;
import android.content.*;import android.media.*;import android.net.Uri;
public final class RingtoneHelper{private static MediaPlayer player;private RingtoneHelper(){} public static synchronized void start(Context c){stop();try{Uri u=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);player=MediaPlayer.create(c,u);if(player!=null){player.setLooping(true);player.start();}}catch(Exception ignored){}} public static synchronized void stop(){try{if(player!=null){if(player.isPlaying())player.stop();player.release();}}catch(Exception ignored){}player=null;}}
