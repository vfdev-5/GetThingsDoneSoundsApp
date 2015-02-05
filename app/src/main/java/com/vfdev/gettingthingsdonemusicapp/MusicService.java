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
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.view.View;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import timber.log.Timber;


/*
   *  Service class to play music from Soundcloud
   *
   *  Example of request :
   *  {
   *  "kind":"track",
   *  "id":3207,
   *  "created_at":"2008/03/04 01:11:02 +0000",
   *  "user_id":1656,
   *  "duration":492800,
   *  "commentable":true,
   *  "state":"finished",
   *  "original_content_size":15872024,
   *  "last_modified":"2011/07/05 15:56:58 +0000",
   *  "sharing":"public",
   *  "tag_list":"\"qburns qburnsabstractmessage redeye doubledown housemusic\"",
   *  "permalink":"party-as-a-verb",
   *  "streamable":true,
   *  "embeddable_by":"all",
   *  "downloadable":false,
   *  "purchase_url":"http://www.stompy.com/catalog/quick-search-results.php?textfield=party+as+a+verb&submit=submit",
   *  "label_id":null,
   *  "purchase_title":null,
   *  "genre":"House",
   *  "title":"Party As A Verb",
   *  "description":"This is Q-Burns Abstract Message in collaboration with Dallas DJ and booty-poet Red Eye. Released on Doubledown Recordings in digital and vinyl formats. It's a club popper!",
   *  "label_name":"Doubledown",
   *  "release":"",
   *  "track_type":"original",
   *  "key_signature":"",
   *  "isrc":null,
   *  "video_url":null,
   *  "bpm":null,
   *  "release_year":null,
   *  "release_month":null,
   *  "release_day":null,
   *  "original_format":"mp3",
   *  "license":"all-rights-reserved",
   *  "uri":"https://api.soundcloud.com/tracks/3207",
   *  "user":{
   *        "id":1656,
   *        "kind":"user",
   *        "permalink":"q-burns-abstract-message",
   *        "username":"Q-Burns Abstract Message",
   *        "last_modified":"2015/01/25 19:38:39 +0000",
   *        "uri":"https://api.soundcloud.com/users/1656",
   *        "permalink_url":"http://soundcloud.com/q-burns-abstract-message",
   *        "avatar_url":"https://i1.sndcdn.com/avatars-000081857164-czaoc6-large.jpg"
   *        },
   *   "permalink_url":"http://soundcloud.com/q-burns-abstract-message/party-as-a-verb",
   *   "artwork_url":null,
   *   "waveform_url":"https://w1.sndcdn.com/YSzOKu308iA3_m.png",
   *   "stream_url":"https://api.soundcloud.com/tracks/3207/stream",
   *   "playback_count":372,
   *   "download_count":0,
   *   "favoritings_count":2,
   *   "comment_count":2,
   *   "attachments_uri":"https://api.soundcloud.com/tracks/3207/attachments",
   *   "policy":"ALLOW"
   *   }
   *
   *
 */

public class MusicService extends Service
        implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener,
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
    private IMusicServiceCallbacks mCallbacks;

    // MediaPlayer
    private MediaPlayer mMediaPlayer;

    // Service states
    private enum State {
        Retrieving, // retrieve track ids from SoundCloud
        Stopped,    // media player is stopped and not prepared to play
        Preparing,  // media player is preparing...
        Playing,    // playback active (media player ready!)
        Paused      // playback paused (media player ready!)
    }
    private boolean hasAudiofocus=false;
    private State mState = State.Stopped;
    // Full state (key-value map) contains (keys) :
    // TrackTitle (String), TrackDuration (Integer), HasPrevTrack (Boolean), IsPlaying (Boolean),
    // TrackLink (String Url)
    private Bundle mFullState;


    // Connection
    private OkHttpClient mSoundCloudClient;
    private String CLIENT_ID;

    private final static int HTTP_OK=200;
    private final static String API_URL="http://api.soundcloud.com/";
    private final static String REQUEST_TRACKS_URL=API_URL+"tracks.json?genres=";
    private final static String REQUEST_TRACKS_URL_WITH_TAGS=API_URL+"tracks.json?tags=";
    private final static String REQUEST_A_TRACK_URL=API_URL+"tracks/";
    private final static String TRACKS_LIMIT="100";

    // Tracks
    private ArrayList<Integer> mTracksHistory;
    private ArrayList<String> mTracks;
    private ArrayList<String> mStyles;
    private String mTags;

    // AsyncTasks
    private DownloadTrackInfo mDownloadTrackInfo;
    private DownloadTrackIds mDownloadTrackIds;
    private DownloadTrackIds2 mDownloadTrackIds2;

    // ImageLoader onLoadingComplete Callback instance
    _SimpleImageLoadingListener mLoadingListener;
    Bitmap mCurrentWaveform;


    // -------- Service methods

    @Override
    public void onCreate() {
        Timber.v("Creating service");

//        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mServiceIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);

        mBinder = new LocalBinder();

        mFullState = new Bundle();

        CLIENT_ID=getString(R.string.soundCloud_client_id);
        mSoundCloudClient = new OkHttpClient();

        mMediaPlayer = new MediaPlayer();

        // Make sure the media player will acquire a wake-lock while playing. If we don't do
        // that, the CPU might go to sleep while the song is playing, causing playback to stop.
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.reset();

        // fetch tracks
        mTracks = new ArrayList<String>();
        mTracksHistory = new ArrayList<Integer>();

        mStyles = new ArrayList<String>();
        mStyles.add("trance");
        mStyles.add("electro");

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

        retrieveTrackIds();
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

        mCallbacks = null;
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

        if (mTracksHistory.size() > 1) {
            mFullState.putBoolean("HasPrevTrack", true);
        } else {
            mFullState.putBoolean("HasPrevTrack", false);
        }

        if (mCallbacks != null) {
            mCallbacks.onMediaPlayerPrepared(mFullState);
        }

        // This handles the case when :
        // audio focus is lost while service was in State.Preparing
        // Player should not start
        if (hasAudiofocus) {
            Timber.v("Has audio focus. Start playing");

            if (mCallbacks != null) {
                mCallbacks.onMediaPlayerStarted();
            }
            player.start();
        } else {
            Timber.v("Has no audio focus. Do not start");
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

    // ---------- Music service callback Interface
    public interface IMusicServiceCallbacks {
        public void onDownloadTrackIdsPostExecute(Bundle result);
        public void onDownloadTrackIdsStarted();
        public void onDownloadTrackInfoPostExecute(Bundle result);
        public void onWaveformLoaded(Bitmap waveform);

        public void onMediaPlayerPrepared(Bundle result);
        public void onMediaPlayerStarted();
        public void onMediaPlayerPaused();
        public void onMediaPlayerIsPreparing();

        public void onShowErrorMessage(String msg);
    }

    // ---------- DownloadTrackIds AsyncTask
    private class DownloadTrackIds extends AsyncTask<Void, Void, Bundle> {
        protected Bundle doInBackground(Void... params) {

            Bundle result = new Bundle();
            for (String style : mStyles) {
                Timber.v("DownloadTrackIds : request track of genre : " + style);

                String requestUrl = REQUEST_TRACKS_URL;
                requestUrl += style;
                requestUrl += "&limit=" + TRACKS_LIMIT;
                requestUrl += "&offset=" + String.valueOf(new Random().nextInt(1000));
                requestUrl += "&client_id=" + CLIENT_ID;

                try {
                    Request request = new Request.Builder()
                            .url(requestUrl).build();

                    Response response = mSoundCloudClient.newCall(request).execute();
                    int code = response.code();
                    String responseStr = response.body().string();
                    if (code == HTTP_OK) {
                        // Parse the response:
                        JSONArray tracksJSON = new JSONArray(responseStr);
                        Timber.v("getTracksInBackground : found " + tracksJSON.length() + " tracks");

                        for (int i = 0; i < tracksJSON.length(); i++) {
                            JSONObject trackJSON = tracksJSON.getJSONObject(i);
                            if (trackJSON.getBoolean("streamable")) {
                                String id = trackJSON.getString("id");
                                mTracks.add(id);
                            }
                        }
                    } else {
                        Timber.e("getTracksInBackground : request error : " + responseStr);

                        result.putBoolean("Result", false);
                        result.putString("Message", getString(R.string.app_err));
                        return result;
                    }
                } catch (IOException e) {
//                    e.printStackTrace();
                    Timber.i(e, "getTracksInBackground : SoundCloud get request error : " + e.getMessage());
                    result.putBoolean("Result", false);
                    result.putString("Message", getString(R.string.connx_err));
                    return result;
                } catch (JSONException e) {
//                    e.printStackTrace();
                    Timber.e(e, "getTracksInBackground : JSON parse error : " + e.getMessage());
                    result.putBoolean("Result", false);
                    result.putString("Message", getString(R.string.app_err));
                    return result;
                }
            }
            result.putBoolean("Result", true);
            result.putString("Message", "");
            return result;

        }

        protected void onPostExecute(Bundle result) {

            if (mCallbacks != null) {
                mCallbacks.onDownloadTrackIdsPostExecute(result);
            }
            mState = State.Stopped;
            mDownloadTrackIds=null;
        }
    }


    // ---------- DownloadTrackIds2 AsyncTask
    private class DownloadTrackIds2 extends AsyncTask<Void, Void, Bundle> {
        protected Bundle doInBackground(Void... params) {

            Bundle result = new Bundle();
            Timber.v("DownloadTrackIds : request tracks on tags : " + mTags);

            String requestUrl = REQUEST_TRACKS_URL_WITH_TAGS;
            requestUrl += mTags;
            requestUrl += "&limit=" + TRACKS_LIMIT;
            requestUrl += "&offset=" + String.valueOf(new Random().nextInt(1000));
            requestUrl += "&client_id=" + CLIENT_ID;

            try {
                Request request = new Request.Builder()
                        .url(requestUrl).build();

                Response response = mSoundCloudClient.newCall(request).execute();
                int code = response.code();
                String responseStr = response.body().string();
                if (code == HTTP_OK) {
                    // Parse the response:
                    JSONArray tracksJSON = new JSONArray(responseStr);
                    Timber.v("getTracksInBackground : found " + tracksJSON.length() + " tracks");

                    for (int i = 0; i < tracksJSON.length(); i++) {
                        JSONObject trackJSON = tracksJSON.getJSONObject(i);
                        if (trackJSON.getBoolean("streamable")) {
                            String id = trackJSON.getString("id");
                            mTracks.add(id);
                        }
                    }
                } else {
                    Timber.e("getTracksInBackground : request error : " + responseStr);

                    result.putBoolean("Result", false);
                    result.putString("Message", getString(R.string.app_err));
                    return result;
                }
            } catch (IOException e) {
//                    e.printStackTrace();
                Timber.i(e, "getTracksInBackground : SoundCloud get request error : " + e.getMessage());
                result.putBoolean("Result", false);
                result.putString("Message", getString(R.string.connx_err));
                return result;
            } catch (JSONException e) {
//                    e.printStackTrace();
                Timber.e(e, "getTracksInBackground : JSON parse error : " + e.getMessage());
                result.putBoolean("Result", false);
                result.putString("Message", getString(R.string.app_err));
                return result;
            }
            result.putBoolean("Result", true);
            result.putString("Message", "");
            return result;

        }

        protected void onPostExecute(Bundle result) {

            if (mCallbacks != null) {
                mCallbacks.onDownloadTrackIdsPostExecute(result);
            }
            mState = State.Stopped;
            mDownloadTrackIds=null;
        }
    }


    // --------- DownloadTrackInfo AsyncTask
    private class DownloadTrackInfo extends AsyncTask<String, Void, Bundle> {
        protected Bundle doInBackground(String... requestUrl) {

            Bundle result = new Bundle();
            try {
                Request request = new Request.Builder()
                        .url(requestUrl[0])
                        .build();
                Response response = mSoundCloudClient.newCall(request).execute();
                int code = response.code();
                String responseStr = response.body().string();
                if (code == HTTP_OK) {
                    result.putBoolean("Result", true);
                    result.putString("JSONResult", responseStr);
                } else {
                    Timber.e("DownloadTrackInfo : request error : " + responseStr);
                    result.putBoolean("Result", false);
                    result.putString("Message", getString(R.string.app_err));
                }
            } catch (IOException e) {
//                e.printStackTrace();
                Timber.i(e, "DownloadTrackInfo : request error : " + e.getMessage());
                result.putBoolean("Result", false);
                result.putString("Message", getString(R.string.connx_err));
            }
            return result;
        }
        protected void onPostExecute(Bundle result) {

            if (!result.getBoolean("Result")) {
                if (mCallbacks!=null) {
                    mCallbacks.onShowErrorMessage(result.getString("Message"));
                }
                mState = State.Stopped;
                mDownloadTrackInfo=null;
                return;
            }

            try {
                JSONObject trackJSON = new JSONObject(result.getString("JSONResult"));
                // prepare player:
                if (trackJSON.getBoolean("streamable")) {
                    String stream_url = trackJSON.getString("stream_url");
                    mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    mMediaPlayer.setDataSource(stream_url + "?client_id=" + CLIENT_ID);
                    mMediaPlayer.prepareAsync();
                }

                // debug : get tags:
                Timber.v("Track tags : "+ trackJSON.getString("tag_list"));

                // get title:
                mFullState.putString("TrackTitle", trackJSON.getString("title"));
                // get permalink:
                mFullState.putString("TrackLink", trackJSON.getString("permalink_url"));
                // get waveform:
                String waveform_url=trackJSON.getString("waveform_url");
                mCurrentWaveform=null;
                ImageLoader.getInstance().loadImage(waveform_url, mLoadingListener);


                if (mCallbacks != null) {
                    mCallbacks.onDownloadTrackInfoPostExecute(mFullState);
                }

            } catch (IOException e) {
//                e.printStackTrace();
                Timber.e(e, "DownloadTrackInfo : onPostExecute : request error : " + e.getMessage());
                if (mCallbacks != null) {
                    mCallbacks.onShowErrorMessage(getString(R.string.app_err));
                }
                mState = State.Stopped;

            } catch (JSONException e) {
//                e.printStackTrace();
                Timber.e(e, "DownloadTrackInfo : onPostExecute : JSON parse error : " + e.getMessage());

                if (mCallbacks != null) {
                    mCallbacks.onShowErrorMessage(getString(R.string.app_err));
                }
                mState = State.Stopped;
            }
            mDownloadTrackInfo=null;
        }
    }

    // ----------- ImageLoader onLoadingComplete listener
    private class _SimpleImageLoadingListener extends SimpleImageLoadingListener {
        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            // Do whatever you want with Bitmap
            mCurrentWaveform = loadedImage;
//            mFullState.putParcelable("TrackWaveform", loadedImage);
            if (mCallbacks != null) {
                mCallbacks.onWaveformLoaded(loadedImage);
            }
        }
    }


    // ------------ Other methods

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

    public void setMusicServiceCallbacks(IMusicServiceCallbacks callbacks) {
        mCallbacks = callbacks;
    }

    public Bundle getCurrentState() {
        if (mState == State.Paused ||
                mState == State.Playing) {
            mFullState.putBoolean("IsPlaying", isPlaying());
        }
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
        if (mCallbacks != null) {
            mCallbacks.onMediaPlayerPaused();
        }

    }

    public boolean play() {

        if (mTracks.size() == 0) {
            retrieveTrackIds();
            return false;
        }

        if (mState == State.Paused) {
            mMediaPlayer.start();
            mState = State.Playing;
            if (mCallbacks != null) {
                mCallbacks.onMediaPlayerStarted();
            }
            return true;
        } else if (mState == State.Stopped) {
            // Only when Service is stopped, request audio focus
            if (requestAudioFocus()) {
                playNextTrack();
                return true;
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

    public void playNextTrack() {

        // Prepare player : get track's info: title, duration, stream_url,  waveform_url
        Timber.v("playNextTrack : DownloadTrackInfo");


        // get track index randomly : it is not in the played tracks history
        int index = -1;
        int count=5;
        while (index < 0) {
            index = new Random().nextInt(mTracks.size());
            if (!mTracksHistory.isEmpty() &&
                    mTracksHistory.contains(index) &&
                    count > 0) {
                index=-1;
            }
            count--;
        }

        prepareAndPlay(index);
        mTracksHistory.add(index);

        if (mTracksHistory.size() > Integer.parseInt(TRACKS_LIMIT)) {
            mTracksHistory.remove(0);
        }

    }

    public void playPrevTrack() {
        if (mTracksHistory.size() > 1) {
            mTracksHistory.remove(mTracksHistory.size() - 1);
            int index = mTracksHistory.get(mTracksHistory.size() - 1);
            prepareAndPlay(index);
        }
    }


    public void rewindTrackTo(int seconds) {
        mMediaPlayer.seekTo(seconds);
    }

    // prepare and play the track at index
    void prepareAndPlay(int trackIndex) {

        if (mDownloadTrackInfo == null) {

            if (mCallbacks != null) {
                mCallbacks.onMediaPlayerIsPreparing();
            }
            mMediaPlayer.reset();
            mState = State.Preparing;

            // Prepare player : get track's info: title, duration, stream_url,  waveform_url
            String requestUrl = REQUEST_A_TRACK_URL;
            requestUrl += mTracks.get(trackIndex) + ".json";
            requestUrl += "?client_id=" + CLIENT_ID;

            mDownloadTrackInfo = new DownloadTrackInfo();
            mDownloadTrackInfo.execute(requestUrl);
        }
    }

    public void setTags(String tags) {
        StringBuilder out= new StringBuilder();
        for (String s : tags.split(",")) {
            String tag=s.toLowerCase();
            tag = tag.replaceAll("^\\s+", "").replaceAll("\\s+$","").replaceAll("\\s","%20");
            out.append(tag).append(",");
        }
        out.deleteCharAt(out.length()-1);
        mTags = out.toString();

    }

    public void retrieveTrackIds() {

        if (mState != State.Retrieving &&
                mTracks.size() == 0 ) {
            Timber.v("Retrieve track ids");

            if (mDownloadTrackIds == null) {
                mState = State.Retrieving;

                if (mCallbacks != null) {
                    mCallbacks.onDownloadTrackIdsStarted();
                }

//                mDownloadTrackIds = new DownloadTrackIds();
//                mDownloadTrackIds.execute();
                mDownloadTrackIds2 = new DownloadTrackIds2();
                mDownloadTrackIds2.execute();

            }

        }

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

            if (mCallbacks != null) {
                mCallbacks.onShowErrorMessage(getString(R.string.no_audio_focus_err));
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





}
