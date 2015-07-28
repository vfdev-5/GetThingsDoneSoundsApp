package com.vfdev.gettingthingsdonemusicapp.Fragments;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.vfdev.gettingthingsdonemusicapp.DB.DBTrackInfo;
import com.vfdev.gettingthingsdonemusicapp.DB.DatabaseHelper;
import com.vfdev.gettingthingsdonemusicapp.R;
import com.vfdev.mimusicservicelib.core.MusicPlayer;
import com.vfdev.mimusicservicelib.core.TrackInfo;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import timber.log.Timber;

/**
 * Fragment to display favorite track
 */
public class FavoriteTracksFragment extends Fragment implements
        ListView.OnItemClickListener,
//        AppDBHandler.OnDBContentChangeListener,
        View.OnClickListener
{

    // UI
    @InjectView(R.id.favoriteTracks)
    protected ListView mFavoriteTracksView;

    @InjectView(R.id.playTracks)
    protected TextView mPlayTracks;

    TranslateAnimation mAnimation;

//    private Drawable mCannotDownload;
//    private Drawable mNotDownloaded;
//    private Drawable mDownloaded;

    // DB handler
    private DatabaseHelper mDBHandler;
    private RuntimeExceptionDao<DBTrackInfo, String> mREDao;

    // Tracks list
    private FavoriteTracksAdapter mAdapter;

    // OnPlayFavoriteTracksListener
    OnPlayFavoriteTracksListener mListener;

    // ------------ Fragment methods

    public FavoriteTracksFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Timber.v("onAttach");

        mAdapter = new FavoriteTracksAdapter(activity);

        // setup button animation
        mAnimation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0.05f);
        mAnimation.setDuration(150);

        mListener = (OnPlayFavoriteTracksListener) activity;

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
        View view = inflater.inflate(R.layout.fragment_favorite_tracks, container, false);
        ButterKnife.inject(this, view);

//        mCannotDownload = getResources().getDrawable(android.R.color.black);
//        mDownloaded = getResources().getDrawable(R.drawable.downloaded);
//        mNotDownloaded = getResources().getDrawable(R.drawable.not_downloaded);

        mREDao = getHelper().getTrackInfoREDao();

        mFavoriteTracksView.setOnItemClickListener(this);
        mFavoriteTracksView.setAdapter(mAdapter);

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
        fillFavoriteTracks(mREDao.queryForAll());
    }

    @Override
    public void onStop() {
        Timber.v("onStop");
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        Timber.v("onDestroyView");
        if (mDBHandler != null) {
            OpenHelperManager.releaseHelper();
            mDBHandler = null;
        }
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

    // ----------- UI

    @OnClick(R.id.playTracks)
    public void onPlayTracks(View view) {
        Timber.v("Play favorite tracks");
        mPlayTracks.startAnimation(mAnimation);
        if (mListener != null) {
            mListener.onPlay(mREDao.queryForAll());
        }
    }

    // ----------- OnDBContentChangeListener

//    @Override
//    public void onDBContentChanged(String table) {
//        if (mAdapter != null) {
//            if (table == AppDBHandler.DB_TABLE_FAVORITE_TRACKS) {
//                fillFavoriteTracks(mDBHandler.getFavoriteTracks());
//            }
//        }
//    }

    // ----------- List item

    /// Parameters
    // parent   :	The AdapterView where the click happened.
    // view     :	The view within the AdapterView that was clicked (this will be a view provided by the adapter)
    // position :	The position of the view in the adapter.
    // id       : 	The row id of the item that was clicked.
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Timber.v("onItemClick");
//        ImageView download = (ImageView) view.findViewById(R.id.ft_item_trackDownload);
//        if (download != null) {
//            FavoriteTrackInfo t = (FavoriteTrackInfo) adapterView.getItemAtPosition(position);
//            onTrackDownloadClicked(download, t);
////            return;
//        }
    }

//    private void onTrackDownloadClicked(ImageView download, DBTrackInfo fTrack) {
//        Timber.v("onTrackDownloadClicked");
//
//        if (star.getDrawable() == mStarOn) {
//            // remove from favorites
//            if (mDBHandler.removeFavoriteTrack(uTrack.id)) {
//                star.setImageDrawable(mStarOff);
//            }
//        } else if (star.getDrawable() == mStarOff) {
//            // add as favorite:
//            if (mDBHandler.addFavoriteTrack(uTrack, "")) {
//                star.setImageDrawable(mStarOn);
//            }
//        }
//    }

    /// Class is OnClickListener of ImageView 'star' -> onClick
    @Override
    public void onClick(View view) {
        Timber.v("onClick");
//        if (view instanceof ImageView) {
//            ImageView download = (ImageView) view;
//            int position = mFavoriteTracksView.getPositionForView(view);
//            onTrackDownloadClicked( download, mAdapter.getItem(position));
//        }
    }

    // ----------- Other methods

//    public void setDBHandler(AppDBHandler handler) {
//        mDBHandler = handler;
//        mDBHandler.setOnDBContentChangeListener(this);
//    }


    private void fillFavoriteTracks(List<DBTrackInfo> tracks){
        Timber.v("fillFavoriteTracks");

        mAdapter.clear();
        mAdapter.addAll(tracks);
    }

    // ----------- OnPlayFavoriteTracksListener
    public interface OnPlayFavoriteTracksListener {
        public void onPlay(List<DBTrackInfo> tracks);
    }


    // ----------- FavoriteTracksAdapter

    private class FavoriteTracksAdapter extends ArrayAdapter<DBTrackInfo> {

        LayoutInflater inflater = getActivity().getLayoutInflater();

        public FavoriteTracksAdapter(Context context) {
            super(context, R.layout.favorite_track_item);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            TrackViewHolder h;

            if (null == convertView) {
                convertView = inflater.inflate(R.layout.favorite_track_item, null);
                h = new TrackViewHolder();
                h.title = (TextView) convertView.findViewById(R.id.ft_item_trackTitle);
                h.tags = (TextView) convertView.findViewById(R.id.ft_item_tags);
                h.duration = (TextView) convertView.findViewById(R.id.ft_item_trackDuration);
//                h.download = (ImageView) convertView.findViewById(R.id.ft_item_trackDownload);
//                h.download.setOnClickListener(FavoriteTracksFragment.this);
                convertView.setTag(h);
            } else {
                h = (TrackViewHolder) convertView.getTag();
            }


            DBTrackInfo t = getItem(position);
            h.title.setText(t.title);
            h.tags.setText(t.tags);
            h.duration.setText(MusicPlayer.getDuration(t.duration));
//            h.download.setImageDrawable(t.pathOnPhone.isEmpty() ? mNotDownloaded : mDownloaded);

            return convertView;
        }

    }

    private static class TrackViewHolder {
        TextView title;
        TextView tags;
        TextView duration;
//        ImageView download;
    }


    // -------------- Get DatabaseHelper
    private DatabaseHelper getHelper() {
        if (mDBHandler == null) {
            mDBHandler = OpenHelperManager.getHelper(getActivity(), DatabaseHelper.class);
        }
        return mDBHandler;
    }


}
