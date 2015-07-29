package com.vfdev.gettingthingsdonemusicapp.Fragments;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.vfdev.gettingthingsdonemusicapp.DB.DBTrackInfo;
import com.vfdev.gettingthingsdonemusicapp.R;
import com.vfdev.gettingthingsdonemusicapp.WaveformView;
import com.vfdev.mimusicservicelib.MusicServiceHelper;
import com.vfdev.mimusicservicelib.core.MusicPlayer;
import com.vfdev.mimusicservicelib.core.TrackInfo;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 * A simple {@link Fragment} subclass.
 */
public class MainFragment extends BaseFragment
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

    // Track countdown timer:
    private _CountDownTimer mTimer;

    // ImageLoader onLoadingComplete Callback instance
    _SimpleImageLoadingListener mLoadingListener;

    // ----------- Fragment methods

    public MainFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Timber.v("onCreateView");

        mLoadingListener = new _SimpleImageLoadingListener();

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

        needRestoreUi = true;

        return view;
    }

    @Override
    public void onPause() {
        Timber.v("onPause");
        super.onPause();
        mTimer.cancel();
    }

    @Override
    public void onResume(){
        Timber.v("onResume");
        super.onResume();

        // restore UI state:
        TrackInfo trackInfo = mMSHelper.getPlayingTrackInfo();
        ArrayList<TrackInfo> trackHistory = mMSHelper.getTracksHistory();
        if (needRestoreUi &&
                trackInfo != null &&
                trackHistory != null){
            restoreUiState(trackInfo, trackHistory.size());
        }
    }

    @Override
    public void onDestroyView() {
        Timber.v("onDestroyView");
        super.onDestroyView();
    }

    // ------ Button callbacks

    @OnClick(R.id.playPauseButton)
    public void onPlayPauseButtonClicked(View view) {
        Timber.v("onPlayPauseButtonClicked");

        // make button responsible :
        if (isPlaying) {
            // is playing -> change icon to 'play' and pause player
            mPlayPauseButton.setImageDrawable(mPlayButtonDrawable);
        } else {
            // is not playing -> change icon to 'pause'
            mPlayPauseButton.setImageDrawable(mPauseButtonDrawable);
        }
        isPlaying=!isPlaying;

        if (mMSHelper.isPlaying()) {
            mMSHelper.pause();
        } else if (!mMSHelper.play()) {
            showMessage(getString(R.string.play_failed));
        }
    }

    @OnClick(R.id.nextTrack)
    public void onNextTrackButtonClicked(View view) {
        Timber.v("onNextTrackButtonClicked");

        // animate button
        mNextTrackButton.startAnimation(mAnimation);

        if (mMSHelper.playNextTrack()) {
            if (!mMSHelper.isPlaying()) {
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

        if (mMSHelper.playPrevTrack()) {
            if (!mMSHelper.isPlaying()) {
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

        TrackInfo trackInfo = mMSHelper.getPlayingTrackInfo();
        openDialogOnTrack(trackInfo);
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

    // ----------- MusicServiceHelper.ReadyEvent

    public void onEvent(MusicServiceHelper.ReadyEvent event) {
        Timber.v("onReady");

        TrackInfo trackInfo = mMSHelper.getPlayingTrackInfo();
        ArrayList<TrackInfo> trackHistory = mMSHelper.getTracksHistory();
        if (needRestoreUi &&
                trackInfo != null &&
                trackHistory != null){
            restoreUiState(trackInfo, trackHistory.size());
        }
    }

    // ----------- MusicPlayer.StateEvent

    public void onEvent(MusicPlayer.StateEvent event) {

        if (event.state == MusicPlayer.State.Playing) {
            Timber.v("onEvent : Playing");
            mTimer.start();
            // synchronize button
            if (mMSHelper.isPlaying()) {
                // is playing -> change icon to 'pause'
                mPlayPauseButton.setImageDrawable(mPauseButtonDrawable);
                isPlaying=true;
            } else  {
                // is not playing -> change icon to 'play'
                mPlayPauseButton.setImageDrawable(mPlayButtonDrawable);
                isPlaying=false;
            }
        } else if (event.state == MusicPlayer.State.Paused) {
            Timber.v("onEvent : Paused");
            mTimer.cancel();
        } else if (event.state == MusicPlayer.State.Preparing) {
            showMessage(getString(R.string.progress_msg2));
            TrackInfo trackInfo = event.trackInfo;
            int tracksHistoryCount = mMSHelper.getTracksHistory().size();
            setupUi(trackInfo, tracksHistoryCount);


        } else if (event.state == MusicPlayer.State.Stopped) {
            // Initialize UI
            mPlayPauseButton.setImageDrawable(mPlayButtonDrawable);
            mTrackWaveform.setVisibility(View.INVISIBLE);
            mTrackTitle.setVisibility(View.INVISIBLE);
            mTrackDuration.setVisibility(View.INVISIBLE);
            mTrackWaveform.setProgress(0.0);
            mTimer.cancel();
        }
    }

    // ----------- Other methods

    private void restoreUiState(TrackInfo trackInfo, int trackHistoryCount) {
        Timber.v("restoreUiState");

        setupUi(trackInfo, trackHistoryCount);

        if (mMSHelper.isPlaying()) {
            mPlayPauseButton.setImageDrawable(mPauseButtonDrawable);
            mTimer.start();
            isPlaying = true;
        } else {
            mPlayPauseButton.setImageDrawable(mPlayButtonDrawable);
            adjustTrackDurationInfo();
            isPlaying = false;
        }
        needRestoreUi=false;
    }

    private void setupUi(TrackInfo trackInfo, int tracksHistoryCount) {
        // Initialize track info and Ui
        Timber.v("setupUi");
        mTrackDuration.setVisibility(View.INVISIBLE);
        mTrackWaveform.setProgress(0.0);
        mTimer.cancel();

        mTrackTitle.setText(trackInfo.title);
        mTrackTitle.setVisibility(View.VISIBLE);
        if (trackInfo.fullInfo.containsKey("permalink_url")) {
            mTrackLink = trackInfo.fullInfo.get("permalink_url");
        }

        if (trackInfo.fullInfo.containsKey("waveform_url")){
            String waveformUrl = trackInfo.fullInfo.get("waveform_url");
            if (!waveformUrl.isEmpty()) {
                ImageLoader.getInstance().loadImage(waveformUrl, mLoadingListener);
            }
        }

        if (trackInfo.duration >= 0) {
            Timber.v("Track duration 1 : " + trackInfo.duration);
            mTrackDuration.setText(getRemainingDuration(trackInfo.duration));
            Timber.v("Track duration 2 : " + mTrackDuration.getText().toString());
            mTrackDuration.setVisibility(View.VISIBLE);
            if (mNextTrackButton.getVisibility() == View.INVISIBLE) {
                mNextTrackButton.setVisibility(View.VISIBLE);
            }
        }

        if (tracksHistoryCount >= 0) {
            if (tracksHistoryCount > 1) {
                mPrevTrackButton.setVisibility(View.VISIBLE);
            } else {
                mPrevTrackButton.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void adjustTrackDurationInfo() {

        if (mMSHelper.getPlayer() != null) {
            int duration = mMSHelper.getPlayer().getTrackDuration();
            int currentPosition = mMSHelper.getPlayer().getTrackCurrentPosition();
            int remaining = duration - currentPosition;
            if (remaining >= 0) {
                mTrackDuration.setText(getRemainingDuration(remaining));
                mTrackWaveform.setProgress(currentPosition * 1.0 / duration);
            }
//            if (currentPosition > 5000 && currentPosition < duration - 5000) {
//                int newPosition = duration - 5000;
//                mMSHelper.getPlayer().rewindTrackTo(newPosition);
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

    @Override
    protected void openDialogOnTrack(final TrackInfo track) {
        // Open Alert dialog :
        // 1) Mark/Unmark as 'Favorite' (!!!)
        // 2) Open in SoundCloud
        // 3) Download to phone (or already downloaded)
        final DBTrackInfo trackInfo = new DBTrackInfo(track);
        final boolean isFavorite = mREDao.idExists(trackInfo.id);
        final CharSequence [] trackChoices = new CharSequence[2];
        if (!isFavorite) {
            trackChoices[0] = getString(R.string.mark_as_favorite);
        } else {
            trackChoices[0] = getString(R.string.remove_from_favorite);
        }

        trackChoices[1] = getString(R.string.open_in_SC);
//        trackChoices[2] = getString(R.string.download);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.on_track_clicked_menu)
                .setItems(trackChoices, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        if (which == 0) {
                            // Mark/Unmark as 'Favorite'
                            if (isFavorite) {
                                daoDeleteById(trackInfo.id);
                            } else {
//                                trackInfo.isStarred = true;
                                daoCreate(trackInfo);
                            }
                        } else if (which == 2) {
                            // Download to phone

                        } else if (which == 1) {
                            // Open in SoundCloud
                            if (trackInfo.trackInfo.fullInfo.containsKey("permalink_url")) {
                                Uri uri = Uri.parse(trackInfo.trackInfo.fullInfo.get("permalink_url"));
                                Intent i = new Intent(Intent.ACTION_VIEW, uri);
                                startActivity(i);
                            }
                        }
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    // ----------- ImageLoader onLoadingComplete listener

    private class _SimpleImageLoadingListener extends SimpleImageLoadingListener {
        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            // Do whatever you want with Bitmap
            mTrackWaveform.setImageBitmap(loadedImage);
            mTrackWaveform.setVisibility(View.VISIBLE);
        }
    }

}
