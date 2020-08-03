package com.example.clipserver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.clipcommon.ClipGenerator;

public class ClipService extends Service {

    private final String TAG = "ClipService";

    private static final int NOTIFICATION_ID = 1;
    private MediaPlayer mPlayer;
    private Notification notification;
    private int length;

    // receives messages from client telling it to stop the service
    private BroadcastReceiver serviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals("StopService")){
                Log.d("broadcast", "message received");
                endService();
            }
        }
    };

    private static String CHANNEL_ID = "Music player style";
    private String[] audioList = {"strongest", "healer", "badnews", "bensound_buddy", "freedom", "dubstep"};

    // Implement the Stub for this Object
    private final ClipGenerator.Stub mBinder = new ClipGenerator.Stub() {

        public void playAudio(int pos){
            //Log.d("Clip pos", "Clip position is " + pos);

            createNotificationChannel();

            Intent notificationIntent = new Intent(getApplicationContext(),
                    ClipService.class);

            PendingIntent pendingIntent = PendingIntent.getActivity(ClipService.this, 0,
                    notificationIntent, 0);

            notification =
                    new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                            .setSmallIcon(android.R.drawable.ic_media_play)
                            .setOngoing(true).setContentTitle(audioList[pos] + " is Playing")
                            .setContentText("Click to Access Music Player")
                            .setTicker("Music is playing!")
                            .setFullScreenIntent(pendingIntent, false)
                            .build();

            // play the media chosen
            // Set up the Media Player
            mPlayer = MediaPlayer.create(ClipService.this, getResources().getIdentifier(audioList[pos], "raw", getPackageName()));
            if (mPlayer != null) {

                mPlayer.setLooping(false);

                // Stop Service when music has finished playing
                mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        stopForeground(true);
                        Intent intent = new Intent("unbind");
                        sendBroadcast(intent);
                    }
                });

                // start the media player
                mPlayer.start();
            }


            // Put this Service in a foreground state, so it won't
            // readily be killed by the system
            startForeground(NOTIFICATION_ID, notification);
        }

        // pause audio
        public void pauseAudio(){
            if(null != mPlayer){
                mPlayer.pause();
                length = mPlayer.getCurrentPosition();
            }
        }

        // stop audio
        public void stopAudio(){
            if(null != mPlayer){
                mPlayer.stop();
                mPlayer.release();
                mPlayer = null;
                stopForeground(true);
            }
        }

        // resume audio
        public void resumeAudio(){
            if(null != mPlayer){
                mPlayer.seekTo(length);
                mPlayer.start();
            }
        }

    };

    @Override
    public void onCreate(){
        super.onCreate();

        IntentFilter intentFilter = new IntentFilter("StopService");
        registerReceiver(serviceReceiver, intentFilter);

        createNotificationChannel();

        notification =
                new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setOngoing(true).setContentTitle("ClipService is running")
                .setContentText("Click to service")
                .build();

        // Don't automatically restart this Service if it is killed
        startForeground(NOTIFICATION_ID, notification);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {
        super.onStartCommand(intent, flags, startid);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != mPlayer) {
            mPlayer.stop();
            mPlayer.release();
        }
        unregisterReceiver(serviceReceiver);
        Log.d("onDestroy", "onDestroy is called");
    }

    public void endService(){
        stopForeground(true);
        stopSelf();
    }

    // UB 11-12-2018:  Now Oreo wants communication channels...
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Music player notification";
            String description = "The channel for music player notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // Return the Stub defined above
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
