package com.vfdev.gettingthingsdonemusicapp.Fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.vfdev.gettingthingsdonemusicapp.Animations.DefaultAnimations;
import com.vfdev.gettingthingsdonemusicapp.DB.DBTrackInfo;
import com.vfdev.gettingthingsdonemusicapp.Dialogs.TrackInfoDialog;
import com.vfdev.gettingthingsdonemusicapp.R;
import com.vfdev.mimusicservicelib.core.MusicPlayer;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import timber.log.Timber;

/**
 * Fragment to display favorite track
 */
public class FavoriteTracksFragment extends BaseFragment implements
        ListView.OnItemClickListener,
        View.OnClickListener
{

    // UI
    @InjectView(R.id.favoriteTracks)
    protected ListView mFavoriteTracksView;

    @InjectView(R.id.playTracks)
    protected TextView mPlayTracks;

    // Animations
    DefaultAnimations mAnimations = new DefaultAnimations();

//    private Drawable mCannotDownload;
//    private Drawable mNotDownloaded;
//    private Drawable mDownloaded;

    // Tracks list
    private FavoriteTracksAdapter mAdapter;

    // ------------ Fragment methods

    public FavoriteTracksFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Timber.v("onAttach");

        mAdapter = new FavoriteTracksAdapter(activity);
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
        mPlayTracks.startAnimation(mAnimations.getButtonAnimation());
        playFavoriteTracks(mREDao.queryForAll());
        showMessage(getString(R.string.ftf_play_tracks_OK));
    }

    // ----------- DBChangedState

    public void onEvent(DBChangedEvent event) {
        if (event.type == DBChangedEvent.EVENT_ITEM_CREATED ||
                event.type == DBChangedEvent.EVENT_ITEM_DELETED) {
            fillFavoriteTracks(mREDao.queryForAll());
            mAdapter.notifyDataSetChanged();
        }
    }

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

        TextView title = (TextView) view.findViewById(R.id.ft_item_trackTitle);
        if (title != null) {
            openDialogOnTrack(mAdapter.getItem(position).trackInfo,
                    TrackInfoDialog.PLAY_BUTTON | TrackInfoDialog.REMOVE_BUTTON);
        }

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


    private void fillFavoriteTracks(List<DBTrackInfo> tracks){
        Timber.v("fillFavoriteTracks");

        mAdapter.clear();
        mAdapter.addAll(tracks);
    }

    private void playFavoriteTracks(List<DBTrackInfo> tracks) {
        mMSHelper.clearPlaylist();
        for (DBTrackInfo t : tracks) {
            mMSHelper.getPlayer().addTrack(t.trackInfo);
        }
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
                h.id = (TextView) convertView.findViewById(R.id.ft_item_id);
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
            h.id.setText(String.valueOf(position+1));
            h.title.setText(t.title);
            h.tags.setText(t.tags.isEmpty() ? getString(R.string.trackinfo_dialog_notags) : t.tags);
            h.duration.setText(MusicPlayer.getDuration(t.duration));
//            h.download.setImageDrawable(t.pathOnPhone.isEmpty() ? mNotDownloaded : mDownloaded);

            return convertView;
        }

    }

    private static class TrackViewHolder {
        TextView id;
        TextView title;
        TextView tags;
        TextView duration;
//        ImageView download;
    }

}
