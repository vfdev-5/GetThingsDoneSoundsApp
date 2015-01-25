package com.vfdev.getworkdonemusicapp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import timber.log.Timber;



/* TODO:
 1) Normal usage
    1.1) Fetch track ids on genres = OK
    1.2) Play/Pause a track = OK
        1.2.1) when track is finished, play next track = OK
    1.3) Play next/previous track = OK
    1.4) Play on background -> Use a service = OK

 2) Abnormal usage
    2.1) No network


 3) BUGS :
     1) Played longtime than stopped with the "Error: what=1, extra=-1004"

 */

public class MainActivity extends Activity implements MusicService.IMusicServiceCallbacks {

    private final static String TAG = MainActivity.class.getName();
    // UI
    private ProgressDialog mProgressDialog;

    @InjectView(R.id.playPauseButton)
    protected ImageButton mPlayPauseButton;

    @InjectView(R.id.nextTrack)
    protected Button mNextTrackButton;
    @InjectView(R.id.prevTrack)
    protected Button mPrevTrackButton;

    @InjectView(R.id.trackTitle)
    protected TextView mTrackTitle;
    @InjectView(R.id.trackDuration)
    protected TextView mTrackDuration;

    @InjectView(R.id.waveform)
    protected WaveformView mTrackWaveform;

    // Service connection
    private MusicService mService;
    private boolean mBound = false;
    private _ServiceConnection mConnection;

    // Track countdown timer:
    private _CountDownTimer mTimer;

    // flag to restore ui at ServiceConnected() or onPause() methods
    private boolean isUiRestored=false;


    // Toast dialog
    private Toast mToast;


    // ------- Activity methods

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        Log.i(TAG, "onCreate");
        Timber.v(TAG + " : " + "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);


        mProgressDialog = new ProgressDialog(this);
        mToast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_LONG);

        // initialize a timer : counter of 10 hours by one second
        // it can be canceled to pause duration counter or restarted
        mTimer = new _CountDownTimer(1000*60*60*10, 1000);

        // create connection
        mConnection = new _ServiceConnection();

        // Image loader
        // Create global configuration and initialize ImageLoader with this config
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this)
                .build();

        ImageLoader.getInstance().init(config);

        startMusicService();

    }

    @Override
    protected void onStart() {
        super.onStart();
//        Log.i(TAG,"onStart");
        Timber.v(TAG + " : " + "onStart");
    }

    @Override
    protected void onStop() {
//        Log.i(TAG,"onStop");
        Timber.v(TAG + " : " + "onStop");
        super.onStop();
    }

    @Override
    protected void onPause(){
//        Log.i(TAG,"onPause");
        Timber.v(TAG + " : " + "onPause");
        super.onPause();

        mTimer.cancel();
        isUiRestored=false;

    }

    @Override
    protected void onResume(){
//        Log.i(TAG,"onResume");
        Timber.v(TAG + " : " + "onResume");
        super.onResume();

        // restore UI state:
        if (mBound) {
            // Restore previous state from MusicService :
            restoreUiState(mService.getCurrentState());
        }


    }

    @Override
    protected void onDestroy() {
//        Log.i(TAG,"onDestroy");
        Timber.v(TAG + " : " + "onDestroy");
        ImageLoader.getInstance().destroy();

        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
        super.onDestroy();
    }

    // ---------- Menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {
            about();
        } else if (id == R.id.action_exit) {
            exit();
            return true;
        }
//        } else if (id == R.id.action_settings) {
//            return true;
//        }



        return super.onOptionsItemSelected(item);
    }

    // ------ Button callbacks
    @OnClick(R.id.playPauseButton)
    public void onPlayPauseButtonClicked(View view) {

//        Log.i(TAG,"onPlayPauseButtonClicked");
        Timber.v(TAG + " : " + "onPlayPauseButtonClicked");

        if (!mBound) {
            Log.e(TAG, "Music service is not bound");
            startMusicService();
            return;
        }

        if (mService.isPlaying()) {
            // is playing -> change icon to 'play' and pause player
            mService.pause();
            mPlayPauseButton.setImageDrawable(getResources().getDrawable(R.drawable.big_play_hover));

        } else if (mService.play()) {
            // is not playing -> change icon to 'pause'
            mPlayPauseButton.setImageDrawable(getResources().getDrawable(R.drawable.big_pause_hover));
        }
    }

    @OnClick(R.id.nextTrack)
    public void onNextTrackButtonClicked(View view) {
//        Log.i(TAG, "onNextTrackButtonClicked");
        Timber.v(TAG + " : " + "onNextTrackButtonClicked");

        if (!mBound) {
            Log.e(TAG, "Music service is not bound");
            startMusicService();
            return;
        }

        if (!mService.isPlaying()) {
            // is not playing -> change icon to 'pause'
            mPlayPauseButton.setImageDrawable(getResources().getDrawable(R.drawable.big_pause_hover));
        }
        mService.playNextTrack();

    }

    @OnClick(R.id.prevTrack)
    public void onPrevTrackButtonClicked(View view) {

//        Log.i(TAG, "onPrevTrackButtonClicked");
        Timber.v(TAG + " : " + "onPrevTrackButtonClicked");

        if (!mBound) {
            Log.e(TAG, "Music service is not bound");
            startMusicService();
            return;
        }

        if (!mService.isPlaying()) {
            // is not playing -> change icon to 'pause'
            mPlayPauseButton.setImageDrawable(getResources().getDrawable(R.drawable.big_pause_hover));
        }
        mService.playPrevTrack();

    }


    // ----------- CountDownTimer
    private class _CountDownTimer extends CountDownTimer {

        public _CountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }
        @Override
        public void onTick(long l) {
            adjustTrackDurationInfo();
        }

        @Override
        public void onFinish() {

        }
    }

    // ----------- Service connection
    // Defines callbacks for service binding, passed to bindService()
    private class _ServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
//            Log.i(TAG,"Main activity is connected to MusicService");
            Timber.v(TAG + " : " + "Main activity is connected to MusicService");

            // We've bound to MusicService, cast the IBinder and get LocalService instance
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            mService = binder.getService();
            mService.setMusicServiceCallbacks(MainActivity.this);
            mBound = true;
            // Start service when bound
            startService(new Intent(MainActivity.this, MusicService.class));

            // Restore previous state from MusicService :
            restoreUiState(mService.getCurrentState());

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
//            Log.i(TAG,"Main activity is disconnected from MusicService");
            Timber.v(TAG + " : " + "Main activity is disconnected from MusicService");

            mBound = false;
        }
    }


    // -------- Music Service callbacks
    @Override
    public void onDownloadTrackIdsPostExecute(Bundle result) {
//        Log.i(TAG, "onDownloadTrackIdsPostExecute");
        Timber.v(TAG + " : " + "onDownloadTrackIdsPostExecute");

        mProgressDialog.dismiss();
        if (!result.getBoolean("Result")) {
            showMessage(result.getString("Message"));
            stopMusicService();
        }
    }

    @Override
    public void onDownloadTrackIdsStarted() {
//        Log.i(TAG, "onDownloadTrackIdsStarted");
        Timber.v(TAG + " : " + "onDownloadTrackIdsStarted");

        startProgressDialog(getString(R.string.progress_msg));
    }

    @Override
    public void onDownloadTrackInfoPostExecute(Bundle trackInfo) {
//        Log.i(TAG, "onDownloadTrackInfoPostExecute");
        Timber.v(TAG + " : " + "onDownloadTrackInfoPostExecute");

        mTrackTitle.setText(trackInfo.getString("TrackTitle"));
        mTrackTitle.setVisibility(View.VISIBLE);

        String waveform_url = trackInfo.getString("TrackWaveformUrl");
        ImageLoader.getInstance().displayImage(waveform_url, mTrackWaveform);
        mTrackWaveform.setVisibility(View.VISIBLE);
    }

    @Override
    public void onMediaPlayerPrepared(Bundle result) {
//        Log.i(TAG,"onMediaPlayerPrepared");
        Timber.v(TAG + " : " + "onMediaPlayerPrepared");

        mTrackDuration.setText(
                getRemainingDuration(result.getInt("TrackDuration"))
        );
        mTrackDuration.setVisibility(View.VISIBLE);

        if (mNextTrackButton.getVisibility() == View.INVISIBLE){
            mNextTrackButton.setVisibility(View.VISIBLE);
        }

        if (result.getBoolean("HasPrevTrack")) {
            mPrevTrackButton.setVisibility(View.VISIBLE);
        } else {
            mPrevTrackButton.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onMediaPlayerStarted() {
//        Log.i(TAG,"onMediaPlayerStarted");
        Timber.v(TAG + " : " + "onMediaPlayerStarted");

        mTimer.start();
    }

    @Override
    public void onMediaPlayerPaused() {
//        Log.i(TAG,"onMediaPlayerPaused");
        Timber.v(TAG + " : " + "onMediaPlayerPaused");

        mTimer.cancel();
    }


    @Override
    public void onMediaPlayerIsPreparing() {
        mTrackDuration.setVisibility(View.INVISIBLE);
        mTrackWaveform.setProgress(0.0);
        mTimer.cancel();
        showMessage(getString(R.string.progress_msg2));
    }

    @Override
    public void onShowErrorMessage(String msg) {
//        Log.i(TAG, "onShowErrorMessage");
        Timber.v(TAG + " : " + "onShowErrorMessage");

        showMessage(msg);
        mPlayPauseButton.setImageDrawable(getResources().getDrawable(R.drawable.big_play_hover));
    }


    // --------- Other class methods

    private void adjustTrackDurationInfo() {
        if (mBound) {
            int duration = mService.getTrackDuration();
            int currentPosition = mService.getTrackCurrentPosition();
            int remaining = duration - currentPosition;
            if (remaining >= 0) {
                mTrackDuration.setText(getRemainingDuration(remaining));
                mTrackWaveform.setProgress(currentPosition*1.0/duration);
            }

//            if (currentPosition > 7000 && currentPosition < duration - 7000) {
//                int newPosition = duration - 7000;
//                mService.rewindTrackTo(newPosition);
//            }
        }
    }

    private void showMessage(String msg) {
        mToast.setText(msg);
        mToast.show();
    }

    private void restoreUiState(Bundle state) {
//        Log.i(TAG, "restoreUiState");
        Timber.v(TAG + " : " + "restoreUiState");

        if (state.isEmpty() || isUiRestored) {
            return;
        }

        onDownloadTrackInfoPostExecute(state);
        onMediaPlayerPrepared(state);

        if (state.getBoolean("IsPlaying")) {
            mPlayPauseButton.setImageDrawable(getResources().getDrawable(R.drawable.big_pause_hover));
            onMediaPlayerStarted();
        } else {
            mPlayPauseButton.setImageDrawable(getResources().getDrawable(R.drawable.big_play_hover));
            adjustTrackDurationInfo();
        }
        isUiRestored=true;
    }

    private String getRemainingDuration(int durationInMillis) {

        int hours = ((int) Math.floor(durationInMillis * 0.001 / 3600.0)) % 60;
        int minutes = ((int) Math.floor(durationInMillis * 0.001 / 60.0)) % 60;
        int seconds = ((int) Math.floor(durationInMillis * 0.001)) % 60;
        if (hours != 0) {
            return String.format("-%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("-%02d:%02d", minutes, seconds);
    }

    private void startProgressDialog(String msg) {
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage(msg);
        mProgressDialog.show();
    }

    private void startMusicService() {
        // Bind to the service
        bindService(new Intent(this, MusicService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    private void stopMusicService() {
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
        stopService(new Intent(MainActivity.this, MusicService.class));
    }

    private void exit() {
//        Log.i(TAG,"exit");
        Timber.v(TAG + " : " + "exit");

        stopMusicService();
        finish();
    }


    void about() {
        About.show(this);
    }

}