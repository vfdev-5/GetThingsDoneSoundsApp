package com.vfdev.gettingthingsdonemusicapp;

import android.app.Activity;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.vfdev.gettingthingsdonemusicapp.core.TrackInfo;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import timber.log.Timber;

/**
 * Playlist fragment
 */
public class PlaylistFragment extends Fragment implements
        MusicService.OnTrackListUpdateListener,
        View.OnClickListener
{

    // UI
    @InjectView(R.id.playlist)
    protected ListView mPlaylistView;

    private Drawable mStarOn;
    private Drawable mStarOff;


    // Playlist
    private PlaylistAdapter mAdapter;

    // Music Service
    private MusicService mService;
    private boolean mBound = false;

    // ----------- Fragment method

    public PlaylistFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Timber.v("onAttach");

        mAdapter = new PlaylistAdapter(activity);

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
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);
        ButterKnife.inject(this, view);

        mStarOn = getResources().getDrawable(android.R.drawable.star_big_on);
        mStarOff = getResources().getDrawable(android.R.drawable.star_big_off);

        return view;
    }

    @Override
    public void onPause() {
        Timber.v("onPause");
        super.onPause();
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
            fillPlaylist(mService.getTrackList());
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

    // ----------- OnTrackListUpdateListener

    @Override
    public void onUpdate(ArrayList<TrackInfo> tracks) {
        Timber.v("onUpdate");
        fillPlaylist(tracks);
    }

    // ----------- List item

    /// this is called when R.id.item_trackStart -> onClick
    @Override
    public void onClick(View view) {

        if (view instanceof ImageView) {
            onTrackStarClicked( (ImageView) view);
        }

    }


    private void onTrackStarClicked(ImageView star) {

        if (star.getDrawable() == mStarOn) {
            star.setImageDrawable(mStarOff);
        } else if (star.getDrawable() == mStarOff) {
            star.setImageDrawable(mStarOn);
        }
    }


    // ----------- Other methods

    public void setService(MusicService service) {
        mService = service;
        mBound = mService != null;
        if (mBound) {
            mService.setTrackListUpdateListener(this);
        }
    }


    private void fillPlaylist(List<TrackInfo> tracks){
        Timber.v("fillPlaylist");

        mAdapter.clear();
        mAdapter.addAll(tracks);
        mPlaylistView.setAdapter(mAdapter);
    }

    private String getDuration(int durationInMillis) {

        int hours = ((int) Math.floor(durationInMillis * 0.001 / 3600.0)) % 60;
        int minutes = ((int) Math.floor(durationInMillis * 0.001 / 60.0)) % 60;
        int seconds = ((int) Math.floor(durationInMillis * 0.001)) % 60;
        if (hours != 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }



    // ----------- PlaylistAdapter

    private class PlaylistAdapter extends ArrayAdapter<TrackInfo> {

        LayoutInflater inflater = getActivity().getLayoutInflater();
//        (LayoutInflater) (this).getSystemService(LAYOUT_INFLATER_SERVICE);

        public PlaylistAdapter(Context context) {
            super(context, R.layout.playlist_item);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            TrackViewHolder h;

            if (null == convertView) {
                convertView = inflater.inflate(R.layout.playlist_item, null);
                h = new TrackViewHolder();
                h.title = (TextView) convertView.findViewById(R.id.item_trackTitle);
                h.tags = (TextView) convertView.findViewById(R.id.item_tags);
                h.duration = (TextView) convertView.findViewById(R.id.item_trackDuration);
                h.star = (ImageView) convertView.findViewById(R.id.item_trackStar);
                h.star.setOnClickListener(PlaylistFragment.this);
                convertView.setTag(h);
            } else {
                h = (TrackViewHolder) convertView.getTag();
            }


            TrackInfo t = getItem(position);
            h.title.setText(t.title);
            h.tags.setText(t.tags);
            h.duration.setText(getDuration(t.duration));
            h.star.setImageDrawable(mStarOff);


            return convertView;
        }

    }

    private static class TrackViewHolder {
        TextView title;
        TextView tags;
        TextView duration;
        ImageView star;
    }



}
