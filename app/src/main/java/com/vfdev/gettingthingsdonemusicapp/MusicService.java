package com.vfdev.gettingthingsdonemusicapp;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.view.View;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.vfdev.gettingthingsdonemusicapp.core.SoundCloudHelper;
import com.vfdev.gettingthingsdonemusicapp.core.TrackInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import timber.log.Timber;




public class MusicService extends Service implements
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
        SoundCloudHelper.OnDownloadTrackInfoListener,
        AudioManager.OnAudioFocusChangeListener {

//    private NotificationManager mNotificationManager;
    private static final int NOTIFICATION_ID = 1;
    private Bitmap mServiceIcon;

    // Wifi manager
    private WifiManager.WifiLock mWifiLock;

    // AudioManager
    private AudioManager mAudioManager;

    // Bound service
    private IBinder mBinder;
    private OnStateChangeListener mListener;
    private OnErrorListener mErrorListener;

    // MediaPlayer
    private MediaPlayer mMediaPlayer;

    // Service states
    private enum State {
        Stopped,    // media player is stopped and not prepared to play
        Preparing,  // media player is preparing...
        Playing,    // playback active (media player ready!)
        Paused      // playback paused (media player ready!)
    }
    private boolean hasAudiofocus=false;
    private State mState = State.Stopped;

    // Full state (key-value map) contains (keys) :
    // TrackTitle (String), TrackDuration (Integer), TrackHistoryCount (Int), TrackLink (String),
    // IsPlaying (Boolean)
    private Bundle mFullState;


    // Connection
    SoundCloudHelper mSoundCloudHelper;

    // Tracks
    private static final int TRACKSHISTORY_LIMIT=50;
    private ArrayList<TrackInfo> mTracksHistory;
    private ArrayList<TrackInfo> mTracks;
//    private TrackInfo mPlayingTrack;

    // ImageLoader onLoadingComplete Callback instance
    _SimpleImageLoadingListener mLoadingListener;
    Bitmap mCurrentWaveform;

    // -------- Service methods

    @Override
    public void onCreate() {
        Timber.v("Creating service");

        mServiceIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);

        mBinder = new LocalBinder();

        mFullState = new Bundle();

        mSoundCloudHelper = SoundCloudHelper.getInstance();
        mSoundCloudHelper.setOnDownloadTrackInfoListener(this);

        mMediaPlayer = new MediaPlayer();

        // Make sure the media player will acquire a wake-lock while playing. If we don't do
        // that, the CPU might go to sleep while the song is playing, causing playback to stop.
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.reset();

        // fetch tracks
        mTracks = new ArrayList<TrackInfo>();
        mTracksHistory = new ArrayList<TrackInfo>();

        // Create the Wifi lock (this does not acquire the lock, this just creates it)
        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
                .createWifiLock(WifiManager.WIFI_MODE_FULL, "MY_WIFI_LOCK");
        mWifiLock.acquire();

        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        // Image loader
        // Create global configuration and initialize ImageLoader with this config
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this)
                .build();
        ImageLoader.getInstance().init(config);
        mLoadingListener = new _SimpleImageLoadingListener();

        showNotification(getString(R.string.no_tracks));
    }

    /**
     * Called when we receive an Intent. When we receive an intent sent to us via startService(),
     * this is the method that gets called. So here we react appropriately depending on the
     * Intent's action, which specifies what is being requested of us.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.v("onStartCommand");

        // on start -> download tracks
        if (mTracks.isEmpty()) {
            mSoundCloudHelper.downloadTrackInfoUsingTags();
        }

        return START_NOT_STICKY; // Don't automatically restart this Service if it is killed
    }

    @Override
    public void onDestroy() {
        // Service is being killed, so make sure we release our resources
        Timber.v("Destroy music service");

        mState = State.Stopped;
        releaseResources();
    }

    @Override
    public IBinder onBind(Intent arg0) {
        Timber.v("onBind");

        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Timber.v("onUnbind");

        mListener = null;
        mErrorListener = null;
        return true;
    }


    // -------- MediaPlayer listeners

    public void onCompletion(MediaPlayer player) {
        Timber.v("onCompletion MediaPlayer");

        // The media player finished playing the current Track, so we go ahead and start the next.
        playNextTrack();
    }

    public void onPrepared(MediaPlayer player) {
        Timber.v("onPrepared MediaPlayer");

        // The media player is done preparing. That means we can start playing!
        mState = State.Playing;
        showNotification(mFullState.getString("TrackTitle"));
        mFullState.putInt("TrackDuration", player.getDuration());

        mFullState.putInt("TrackHistoryCount", mTracksHistory.size());

        if (mListener != null) {
            mListener.onPrepared(mFullState);
        }

        // This handles the case when :
        // audio focus is lost while service was in State.Preparing
        // Player should not start
        if (hasAudiofocus) {
            Timber.v("Has audio focus. Start playing");

            if (mListener != null) {
                mListener.onStarted();
            }
            player.start();
        } else {
            Timber.v("Has no audio focus. Do not start");
        }

        // get tracks if empty
        if (mTracks.isEmpty()) {
            mSoundCloudHelper.downloadTrackInfoUsingTags();
        }
    }

    public boolean onError(MediaPlayer mp, int what, int extra) {
        Timber.i("what=" + String.valueOf(what) + ", extra=" + String.valueOf(extra));

        mState = State.Stopped;
        return true; // true indicates we handled the error
    }

    // ------------ Local binder

    public class LocalBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    // ------------ OnAudioFocusChangeListener
    @Override
    public void onAudioFocusChange(int focusChange) {
        Timber.v("onAudioFocusChange");

        if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            Timber.v("AUDIOFOCUS_LOSS_TRANSIENT");

            hasAudiofocus=false;
            // Pause playback
            if (mMediaPlayer.isPlaying()) {
                pause();
                mState = State.Playing;
            }
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            Timber.v("AUDIOFOCUS_GAIN");

            // Resume playback
            hasAudiofocus=true;
            if (!mMediaPlayer.isPlaying() &&
                    mState == State.Playing) {
                mState = State.Paused;
                play();
            }
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            Timber.v("AUDIOFOCUS_LOSS");

            // Audio focus loss is permanent
            releaseAudioFocus();
        }
    }

    // ---------- Music service Listener Interface

    public interface OnStateChangeListener {
        public void onWaveformLoaded(Bitmap waveform);
        public void onPrepared(Bundle result);
        public void onStarted();
        public void onPaused();
        public void onIsPreparing();
        public void onStopped();

    }

    public interface OnErrorListener {
        public void onShowErrorMessage(String msg);
    }

    // ----------- ImageLoader onLoadingComplete listener

    private class _SimpleImageLoadingListener extends SimpleImageLoadingListener {
        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            // Do whatever you want with Bitmap
            mCurrentWaveform = loadedImage;
            if (mListener != null) {
                mListener.onWaveformLoaded(loadedImage);
            }
        }
    }

    // ------------ OnDownloadTrackInfoListener

    @Override
    public void onDownloadTrackInfo(Bundle result, ArrayList<TrackInfo> tracks) {
        Timber.v("onDownloadTrackInfo");
        if (result.getBoolean("Result")) {
            mTracks.addAll(tracks);
            Timber.v("Add tracks -> tracklist size = " + mTracks.size());
        } else {

            if (mErrorListener != null) {
                int errorType = result.getInt("ErrorType");
                if (errorType == SoundCloudHelper.APP_ERR) {
                    mErrorListener.onShowErrorMessage(getString(R.string.app_err));
                } else if (errorType == SoundCloudHelper.CONNECTION_ERR) {
                    mErrorListener.onShowErrorMessage(getString(R.string.connx_err));
                }
            }

            mState = State.Stopped;
            if (mListener != null) {
                mListener.onStopped();
            }
        }
    }


    // ------------ Other methods

    public void setStateChangeListener(OnStateChangeListener listener) {
        mListener = listener;
    }

    public void setErrorListener(OnErrorListener listener) {
        mErrorListener = listener;
    }

    public void setTracks(ArrayList<TrackInfo> tracks) {
        mTracks = tracks;
    }

    public Bundle getCurrentState() {
        return mFullState;
    }

    public Bitmap getCurrentWaveform() {
        return mCurrentWaveform;
    }

    public boolean isPlaying() {
        return mState == State.Playing;
    }

    public void pause(){
        mMediaPlayer.pause();
        mState = State.Paused;
        if (mListener != null) {
            mListener.onPaused();
        }
    }

    public boolean play() {
        Timber.v("play");

        if (mState == State.Paused) {
            mMediaPlayer.start();
            mState = State.Playing;
            if (mListener != null) {
                mListener.onStarted();
            }
            return true;
        } else if (mState == State.Stopped) {
            // Only when Service is stopped, request audio focus
            if (requestAudioFocus()) {
                return playNextTrack();
            }
        }

        return false;
    }

    public int getTrackDuration() {
        return mMediaPlayer.getDuration();
    }

    public int getTrackCurrentPosition() {
        return mMediaPlayer.getCurrentPosition();
    }

    public boolean playNextTrack() {

        // Prepare player : get track's info: title, duration, stream_url,  waveform_url
        Timber.v("playNextTrack");

        // this should solve the problem of multiple 'machine gun' touches problem
        if (mState == State.Preparing)
            return true;

        if (mTracks.isEmpty()) {
            Timber.v("TRACK LIST IS EMPTY");
            return false;
        }

        toPreparingState();

        // get track index randomly :
        debugShowTracks();
        int index = new Random().nextInt(mTracks.size());
        TrackInfo track = mTracks.remove(index);
        debugShowTracks();
        mTracksHistory.add(track);

        if (mTracksHistory.size() > TRACKSHISTORY_LIMIT) {
            mTracksHistory.remove(0);
        }
        return prepareAndPlay(track);

    }

    private void debugShowTracks() {
        Timber.d("Tracks : ------- ");
        for (TrackInfo track  : mTracks) {
            Timber.v("\t " + track.id + " | \t" + track.title);
        }
        Timber.d("---------------- ");
    }

    public boolean playPrevTrack() {
        Timber.v("playPrevTrack");

        // this should solve the problem of multiple 'machine gun' touches problem
        if (mState == State.Preparing)
            return true;

        if (mTracksHistory.size() > 1) {

            toPreparingState();
            mTracksHistory.remove(mTracksHistory.size() - 1);
            TrackInfo track = mTracksHistory.get(mTracksHistory.size() - 1);
            return prepareAndPlay(track);
        }
        return false;
    }

    public void rewindTrackTo(int seconds) {
        mMediaPlayer.seekTo(seconds);
    }

    private void toPreparingState() {
        if (mListener != null) {
            mListener.onIsPreparing();
        }
        mState = State.Preparing;
        mFullState.clear();
        mMediaPlayer.reset();
    }

    private boolean prepareAndPlay(TrackInfo track) {

        try {

            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setDataSource(mSoundCloudHelper.getCompleteStreamUrl(track.streamUrl));
            mMediaPlayer.prepareAsync();

        } catch (IOException e) {

            Timber.e(e, "DownloadTrackInfo : onPostExecute : request error : " + e.getMessage());
            if (mErrorListener != null) {
                mErrorListener.onShowErrorMessage(getString(R.string.app_err));
            }
            if (mListener != null) {
                mListener.onStopped();
            }
            mState = State.Stopped;
            return false;
        }

        // debug : get tags:
        Timber.v("Track title : "+ track.title);
        Timber.v("Track tags : "+ track.tags);

        // get title:
        mFullState.putString("TrackTitle", track.title);
        // get permalink:
        mFullState.putString("TrackLink", track.soundcloudUrl);
        // get waveform:
        String waveform_url=track.waveformUrl;
        mCurrentWaveform=null;
        ImageLoader.getInstance().loadImage(waveform_url, mLoadingListener);

        if (mListener != null) {
            mListener.onPrepared(mFullState);
        }
        return true;
    }

    private boolean requestAudioFocus() {
        // request audio focus :
        int result = mAudioManager.requestAudioFocus(this,
                // Use the music stream.
                AudioManager.STREAM_MUSIC,
                // Request permanent focus.
                AudioManager.AUDIOFOCUS_GAIN);

        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Timber.v("Audio focus request is not granted");

            if (mErrorListener != null) {
                mErrorListener.onShowErrorMessage(getString(R.string.no_audio_focus_err));
            }
            hasAudiofocus=false;
        } else {
            hasAudiofocus = true;
        }
        return hasAudiofocus;
    }

    private void releaseAudioFocus() {
        hasAudiofocus=false;
        mAudioManager.abandonAudioFocus(this);
    }

    private void releaseResources() {

        // stop being a foreground service
        stopForeground(true);

        // stop and release the Media Player, if it's available
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        // we can also release the Wifi lock, if we're holding it
        if (mWifiLock.isHeld()) mWifiLock.release();

        // release audio focus
        releaseAudioFocus();

        // destroy imageloader
        ImageLoader.getInstance().destroy();

    }

    private void showNotification(String trackTitle) {

        Timber.v("Show notification");

        // Create a notification area notification so the user
        // can get back to the MainActivity
        final Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Context c = getApplicationContext();

        final Notification notification = new Notification.Builder(getApplicationContext())
                .setContentTitle(getString(R.string.app_name))
                .setContentText(trackTitle)
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(mServiceIcon)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();

        // Put this Service in a foreground state, so it won't
        // readily be killed by the system
        startForeground(NOTIFICATION_ID, notification);

    }

}
