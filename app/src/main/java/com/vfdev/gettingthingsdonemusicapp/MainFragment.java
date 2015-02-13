package com.vfdev.gettingthingsdonemusicapp;


import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.vfdev.gettingthingsdonemusicapp.R;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import timber.log.Timber;

/**
 * A simple {@link Fragment} subclass.
 */
public class MainFragment extends Fragment implements
        MusicService.OnStateChangeListener
{


    // Ui
    @InjectView(R.id.playPauseButton)
    protected ImageButton mPlayPauseButton;
    private boolean isPlaying=false;

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

    // Toast dialog
    private Toast mToast;

    // flag to restore ui at ServiceConnected() or onPause() methods
    private boolean isUiRestored=false;

    // Track countdown timer:
    private _CountDownTimer mTimer;

    // Service connection
    private MusicService mService;
    private boolean mBound = false;

    // ----------- Fragment methods

    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Timber.v("onAttach");
        mToast = Toast.makeText(activity.getApplicationContext(), "", Toast.LENGTH_LONG);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Timber.v("onCreate");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Timber.v("onCreateView");

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        ButterKnife.inject(this, view);

        // setup play/pause drawables
        mPlayButtonDrawable = getResources().getDrawable(R.drawable.play_button);
        mPauseButtonDrawable = getResources().getDrawable(R.drawable.pause_button);
        mPlayPauseButton.setImageDrawable(mPlayButtonDrawable);


        // initialize a timer : counter of 10 hours by one second
        // it can be canceled to pause duration counter or restarted
        mTimer = new _CountDownTimer(1000*60*60*10, 1000);

        // setup button animation
        mAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0.05f);
        mAnimation.setDuration(150);

        return view;
    }

    @Override
    public void onPause() {
        Timber.v("onPause");
        super.onPause();

        mTimer.cancel();
        isUiRestored=false;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        Timber.v("onActivityCreated");
    }

    @Override
    public void onStart() {
        super.onStart();
        Timber.v("onStart");
    }

    @Override
    public void onResume(){
        Timber.v("onResume");
        super.onResume();

        // restore UI state:
        if (mBound) {
            // Restore previous state from MusicService :
            restoreUiState(mService.getCurrentState(), mService.getCurrentWaveform());
        }
    }

    @Override
    public void onStop() {
        Timber.v("onStop");
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        Timber.v("onDestroyView");
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        Timber.v("onDestroy");
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        Timber.v("onDetach");
        super.onDetach();
    }
    
    // ------ Button callbacks

    @OnClick(R.id.playPauseButton)
    public void onPlayPauseButtonClicked(View view) {
        Timber.v("onPlayPauseButtonClicked");

        if (!mBound) {
            Timber.v("Play track is clicked, but MusicService is not bound -> startMusicService");
//            startMusicService();
            return;
        }

        // make button responsible :
        if (isPlaying) {
            // is playing -> change icon to 'play' and pause player
            mPlayPauseButton.setImageDrawable(mPlayButtonDrawable);
        } else {
            // is not playing -> change icon to 'pause'
            mPlayPauseButton.setImageDrawable(mPauseButtonDrawable);
        }
        isPlaying=!isPlaying;

        if (mService.isPlaying()) {
            mService.pause();
        } else if (!mService.play()) {
            showMessage(getString(R.string.play_failed));
        }
    }

    @OnClick(R.id.nextTrack)
    public void onNextTrackButtonClicked(View view) {
        Timber.v("onNextTrackButtonClicked");

        // animate button
        mNextTrackButton.startAnimation(mAnimation);

        if (!mBound) {
            Timber.v("Next track is clicked, but MusicService is not bound -> startMusicService");
//            startMusicService();
            return;
        }

        if (mService.playNextTrack()) {
            if (!mService.isPlaying()) {
                // is not playing -> change icon to 'pause'
                mPlayPauseButton.setImageDrawable(mPauseButtonDrawable);
                isPlaying=true;
            }
        } else {
            showMessage(getString(R.string.nexttrack_err));
        }

    }

    @OnClick(R.id.prevTrack)
    public void onPrevTrackButtonClicked(View view) {
        Timber.v("onPrevTrackButtonClicked");

        // animate button
        mPrevTrackButton.startAnimation(mAnimation);

        if (!mBound) {
            Timber.v("Prev track is clicked, but MusicService is not bound -> startMusicService");
//            startMusicService();
            return;
        }

        if (mService.playPrevTrack()) {
            if (!mService.isPlaying()) {
                // is not playing -> change icon to 'pause'
                mPlayPauseButton.setImageDrawable(mPauseButtonDrawable);
                isPlaying=true;
            }
        } else {
            showMessage(getString(R.string.prevtrack_err));
        }

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

    // ----------- MusicService . OnStateChangeListener

//    @Override
//    public void onDownloadTrackInfoPostExecute(Bundle result) {
//
//    }

    @Override
    public void onWaveformLoaded(Bitmap waveform) {
        mTrackWaveform.setImageBitmap(waveform);
        mTrackWaveform.setVisibility(View.VISIBLE);
    }

    @Override
    public void onStopped() {
        // Initialize UI
        mPlayPauseButton.setImageDrawable(mPlayButtonDrawable);
        mTrackWaveform.setVisibility(View.INVISIBLE);
        mTrackTitle.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onPrepared(Bundle serviceState) {

        if (serviceState.containsKey("TrackTitle")) {
            mTrackTitle.setText(serviceState.getString("TrackTitle"));
            mTrackTitle.setVisibility(View.VISIBLE);
        }

        if (serviceState.containsKey("TrackLink")) {
            mTrackLink = serviceState.getString("TrackLink");
        }

        if (serviceState.containsKey("TrackDuration")) {
            mTrackDuration.setText(
                    getRemainingDuration(serviceState.getInt("TrackDuration"))
            );
            mTrackDuration.setVisibility(View.VISIBLE);

            if (mNextTrackButton.getVisibility() == View.INVISIBLE){
                mNextTrackButton.setVisibility(View.VISIBLE);
            }
        }

        if (serviceState.containsKey("TrackHistoryCount")) {
            if (serviceState.getInt("TrackHistoryCount") > 1) {
                mPrevTrackButton.setVisibility(View.VISIBLE);
            } else {
                mPrevTrackButton.setVisibility(View.INVISIBLE);
            }
        }
    }


    @Override
    public void onStarted() {
        Timber.v("onStarted");
        mTimer.start();

        // synchronize button
        if (mService.isPlaying()) {
            // is playing -> change icon to 'pause'
            mPlayPauseButton.setImageDrawable(mPauseButtonDrawable);
            isPlaying=true;
        } else  {
            // is not playing -> change icon to 'play'
            mPlayPauseButton.setImageDrawable(mPlayButtonDrawable);
            isPlaying=false;
        }


    }

    @Override
    public void onPaused() {
        Timber.v("onPaused");
        mTimer.cancel();
    }

    @Override
    public void onIsPreparing() {
        // Initialize track info and Ui
        Timber.v("onIsPreparing");
        showMessage(getString(R.string.progress_msg2));
        mTrackDuration.setVisibility(View.INVISIBLE);
        mTrackWaveform.setProgress(0.0);
        mTimer.cancel();
    }

    // ----------- Other methods

    public void setService(MusicService service) {
        mService = service;
        mBound = mService != null;
        if (mBound) {
            mService.setStateChangeListener(this);
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

        onPrepared(state);
        if (waveform != null) {
            onWaveformLoaded(waveform);
        }

        if (mService.isPlaying()) {
            mPlayPauseButton.setImageDrawable(mPauseButtonDrawable);
            mTimer.start();
        } else {
            mPlayPauseButton.setImageDrawable(mPlayButtonDrawable);
            adjustTrackDurationInfo();
        }
        isUiRestored=true;
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

//            if (currentPosition > 5000 && currentPosition < duration - 5000) {
//                int newPosition = duration - 5000;
//                mService.rewindTrackTo(newPosition);
//            }
        }
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



}
