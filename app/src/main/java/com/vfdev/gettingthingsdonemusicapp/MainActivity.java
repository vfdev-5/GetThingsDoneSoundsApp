package com.vfdev.gettingthingsdonemusicapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import timber.log.Timber;



/* TODO:
 1) Normal usage

    ---- version 1.0

    1.1) Fetch track ids on genres = OK
    1.2) Play/Pause a track = OK
        1.2.1) when track is finished, play next track = OK
    1.3) Play next/previous track = OK
    1.4) Play on background -> Use a service = OK
    1.5) Save track waveform in the service and do not reload from URL on activity UI restore = OK
    1.6) Visual response on press next/prev track buttons = OK
    1.7) Click on title -> open track in the browser = OK

    ---- version 1.1
    1.8) Random choice of track and do not repeat : check id in track history
    1.9) Settings : configure retrieved styles by keywords (default: trance, electro)
        - replace search genres by tags:
        default tags : trance,electronic,armin,Dash Berlin,ASOT

    ---- version 2.0
    2.0) View Pager : view1 = Main, view2 = List of played tracks, view3 = Favorite tracks
    2.1) Add local DB to store conf:
    2.2) Download playing track
    2.3) Show played list : list item = { track name }
    2.4) 'Like' track


 2) Abnormal usage
    2.1) No network

 3) BUGS :

 */

public class MainActivity extends Activity implements
        MusicService.IMusicServiceCallbacks,
        ServiceConnection,
        SettingsDialog.SettingsDialogCallback
{

    // UI
    private ProgressDialog mProgressDialog;

    @InjectView(R.id.playPauseButton)
    protected ImageButton mPlayPauseButton;

    @InjectView(R.id.nextTrack)
    protected Button mNextTrackButton;
    @InjectView(R.id.prevTrack)
    protected Button mPrevTrackButton;

    private Drawable mPlayButtonDrawable;
    private Drawable mPauseButtonDrawable;

    // Button animation (next/prev track)
    TranslateAnimation mAnimation;

    @InjectView(R.id.trackTitle)
    protected TextView mTrackTitle;
    String mTrackLink = "";

    @InjectView(R.id.trackDuration)
    protected TextView mTrackDuration;

    @InjectView(R.id.waveform)
    protected WaveformView mTrackWaveform;

    // Service connection
    private MusicService mService;
    private boolean mBound = false;

    // Track countdown timer:
    private _CountDownTimer mTimer;

    // flag to restore ui at ServiceConnected() or onPause() methods
    private boolean isUiRestored=false;

    // Toast dialog
    private Toast mToast;

    // Database handler
    AppDBHandler mDBHandler;


    // ------- Activity methods

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timber.v("onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        // set custom title :
        setTitle(R.string.main_activity_name);

        // setup play/pause drawables
        mPlayButtonDrawable = getResources().getDrawable(R.drawable.play_button);
        mPauseButtonDrawable = getResources().getDrawable(R.drawable.pause_button);
        mPlayPauseButton.setImageDrawable(mPlayButtonDrawable);

        mProgressDialog = new ProgressDialog(this);
        mToast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_LONG);

        // initialize a timer : counter of 10 hours by one second
        // it can be canceled to pause duration counter or restarted
        mTimer = new _CountDownTimer(1000*60*60*10, 1000);

        // setup button animation
        mAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0.05f);
        mAnimation.setDuration(150);


        setupDB();

        startMusicService();

    }

    @Override
    protected void onStart() {
        super.onStart();
        Timber.v("onStart");
    }

    @Override
    protected void onStop() {
        Timber.v("onStop");
        super.onStop();
    }

    @Override
    protected void onPause(){
        Timber.v("onPause");
        super.onPause();

        mTimer.cancel();
        isUiRestored=false;

    }

    @Override
    protected void onResume(){
        Timber.v("onResume");
        super.onResume();

        // restore UI state:
        if (mBound) {
            // Restore previous state from MusicService :
            restoreUiState(mService.getCurrentState(), mService.getCurrentWaveform());
        }


    }

    @Override
    protected void onDestroy() {
        Timber.v("onDestroy");

        // Unbind from the service
        if (mBound) {
//            unbindService(mConnection);
            unbindService(this);
            mBound = false;
        }

        // Close database connection
        mDBHandler.close();

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
        } else if (id == R.id.action_settings) {
            settings();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // ------ Button callbacks
    @OnClick(R.id.playPauseButton)
    public void onPlayPauseButtonClicked(View view) {
        Timber.v("onPlayPauseButtonClicked");

        if (!mBound) {
            Timber.v("Play track is clicked, but MusicService is not bound -> startMusicService");
            startMusicService();
            return;
        }

        if (mService.isPlaying()) {
            // is playing -> change icon to 'play' and pause player
            mService.pause();
            mPlayPauseButton.setImageDrawable(mPlayButtonDrawable);

        } else if (mService.play()) {
            // is not playing -> change icon to 'pause'
            mPlayPauseButton.setImageDrawable(mPauseButtonDrawable);
        }
    }

    @OnClick(R.id.nextTrack)
    public void onNextTrackButtonClicked(View view) {
        Timber.v("onNextTrackButtonClicked");

        // animate button
        mNextTrackButton.startAnimation(mAnimation);

        if (!mBound) {
            Timber.v("Next track is clicked, but MusicService is not bound -> startMusicService");
            startMusicService();
            return;
        }

        if (!mService.isPlaying()) {
            // is not playing -> change icon to 'pause'
            mPlayPauseButton.setImageDrawable(mPauseButtonDrawable);
        }
        mService.playNextTrack();

    }

    @OnClick(R.id.prevTrack)
    public void onPrevTrackButtonClicked(View view) {
        Timber.v("onPrevTrackButtonClicked");

        // animate button
        mPrevTrackButton.startAnimation(mAnimation);

        if (!mBound) {
            Timber.v("Prev track is clicked, but MusicService is not bound -> startMusicService");
            startMusicService();
            return;
        }

        if (!mService.isPlaying()) {
            // is not playing -> change icon to 'pause'
            mPlayPauseButton.setImageDrawable(mPauseButtonDrawable);
        }
        mService.playPrevTrack();

    }

    @OnClick(R.id.trackTitle)
    public void onTrackTitleClicked(View view) {
        Timber.v("onTrackTitleClicked");
        if (!mTrackLink.isEmpty()) {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(mTrackLink));
            startActivity(i);
        }
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
    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
        Timber.v("Main activity is connected to MusicService");

        // We've bound to MusicService, cast the IBinder and get LocalService instance
        MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
        mService = binder.getService();
        mService.setMusicServiceCallbacks(MainActivity.this);
        mBound = true;

        // set tags:
        mService.setTags(mDBHandler.getTags());

        // Start service when bound
        startService(new Intent(MainActivity.this, MusicService.class));

        // Restore previous state from MusicService :
        restoreUiState(mService.getCurrentState(), mService.getCurrentWaveform());

    }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
        Timber.v("Main activity is disconnected from MusicService");

        mBound = false;
    }
//    }

    // -------- Music Service callbacks
    @Override
    public void onDownloadTrackIdsPostExecute(Bundle result) {
        Timber.v("onDownloadTrackIdsPostExecute");

        mProgressDialog.dismiss();
        if (!result.getBoolean("Result")) {
            showMessage(result.getString("Message"));
            stopMusicService();
        }
    }

    @Override
    public void onDownloadTrackIdsStarted() {
        Timber.v("onDownloadTrackIdsStarted");

        startProgressDialog(getString(R.string.progress_msg));
    }

    @Override
    public void onDownloadTrackInfoPostExecute(Bundle trackInfo) {
        Timber.v("onDownloadTrackInfoPostExecute");

        mTrackTitle.setText(trackInfo.getString("TrackTitle"));
        mTrackTitle.setVisibility(View.VISIBLE);
        mTrackLink = trackInfo.getString("TrackLink");
    }

    @Override
    public void onWaveformLoaded(Bitmap waveform) {
        Timber.v("onWaveformLoaded");
        mTrackWaveform.setImageBitmap(waveform);
        mTrackWaveform.setVisibility(View.VISIBLE);
    }


    @Override
    public void onMediaPlayerPrepared(Bundle result) {
        Timber.v("onMediaPlayerPrepared");

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
        Timber.v("onMediaPlayerStarted");

        mTimer.start();
    }

    @Override
    public void onMediaPlayerPaused() {
        Timber.v("onMediaPlayerPaused");

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
        Timber.v("onShowErrorMessage");

        showMessage(msg);
        initUiState();
    }


    // --------- Other class methods

    private void initUiState() {
        mPlayPauseButton.setImageDrawable(mPlayButtonDrawable);
        mTrackWaveform.setVisibility(View.INVISIBLE);
        mTrackTitle.setVisibility(View.INVISIBLE);
    }

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

    private void restoreUiState(Bundle state, Bitmap waveform) {
        Timber.v("restoreUiState");

        if (state.isEmpty() || isUiRestored) {
            return;
        }

        onDownloadTrackInfoPostExecute(state);
        onMediaPlayerPrepared(state);
        if (waveform != null) {
            onWaveformLoaded(waveform);
        }

        if (state.getBoolean("IsPlaying")) {
            mPlayPauseButton.setImageDrawable(mPauseButtonDrawable);
            onMediaPlayerStarted();
        } else {
            mPlayPauseButton.setImageDrawable(mPlayButtonDrawable);
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

    private void setupDB() {
        Timber.v("Setup DB");
        mDBHandler = new AppDBHandler(this);
    }

    private void startMusicService() {
        // Bind to the service
        bindService(new Intent(this, MusicService.class), this, Context.BIND_AUTO_CREATE);
    }

    private void stopMusicService() {
        if (mBound) {
            unbindService(this);
            mBound = false;
        }
        stopService(new Intent(MainActivity.this, MusicService.class));
    }

    private void exit() {
        Timber.v("exit");

        stopMusicService();
        finish();
    }


    private void about() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.about_dialog_title)
                .setView(getLayoutInflater().inflate(R.layout.alertdialog_about, null));
        AlertDialog dialog = builder.create();
        dialog.show();

    }

//    private void settings() {
//        new MenuDialogHelper().showSettings(this, mDBHandler);
//    }

    private void settings() {

        SettingsDialog dialog = new SettingsDialog(this);
        dialog.setData(mDBHandler.getTags());
        dialog.show();

    }

    @Override
    public void onUpdateData(String newTags) {
        mDBHandler.setTags(newTags);
        Toast.makeText(this, getString(R.string.tags_updated), Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onResetDefault() {
        mDBHandler.setTags(getString(R.string.settings_default_tags));
        Toast.makeText(this, getString(R.string.tags_default), Toast.LENGTH_SHORT).show();
    }


}