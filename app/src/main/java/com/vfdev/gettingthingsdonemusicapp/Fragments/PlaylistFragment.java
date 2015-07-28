package com.vfdev.gettingthingsdonemusicapp.Fragments;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;
import timber.log.Timber;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.vfdev.gettingthingsdonemusicapp.DB.DBTrackInfo;
import com.vfdev.gettingthingsdonemusicapp.DB.DatabaseHelper;
import com.vfdev.gettingthingsdonemusicapp.R;
import com.vfdev.mimusicservicelib.MusicService;
import com.vfdev.mimusicservicelib.MusicServiceHelper;
import com.vfdev.mimusicservicelib.core.MusicPlayer;
import com.vfdev.mimusicservicelib.core.TrackInfo;

/**
 * Playlist fragment
 */
public class PlaylistFragment extends Fragment implements
//        MusicService.OnTrackListUpdateListener,
        ListView.OnItemClickListener,
        View.OnClickListener
{

    // UI
    @InjectView(R.id.playlist)
    protected ListView mPlaylistView;

    private Drawable mStarOn;
    private Drawable mStarOff;

    // DB handler
    private DatabaseHelper mDBHandler;
    private RuntimeExceptionDao<DBTrackInfo, String> mREDao;

    // Playlist
    private PlaylistAdapter mAdapter;

    // Music Service
    private MusicServiceHelper mMSHelper;


    private boolean needRestoreUi = false;

    // ----------- Fragment method

    public PlaylistFragment() {
        // Required empty public constructor
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Timber.v("onAttach");

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

        EventBus.getDefault().register(this);
        mAdapter = new PlaylistAdapter(getActivity());
        mREDao = getHelper().getTrackInfoREDao();

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);
        ButterKnife.inject(this, view);

        mStarOn = getResources().getDrawable(android.R.drawable.star_big_on);
        mStarOff = getResources().getDrawable(android.R.drawable.star_big_off);

        mPlaylistView.setOnItemClickListener(this);
        mPlaylistView.setAdapter(mAdapter);

        needRestoreUi = true;

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
        if (!checkMusicServiceHelper()) return;
        ArrayList<TrackInfo> trackHistory = mMSHelper.getTracksHistory();
        if (needRestoreUi &&
                trackHistory != null) {
            fillPlaylist(trackHistory);
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

        EventBus.getDefault().unregister(this);
        if (mDBHandler != null) {
            OpenHelperManager.releaseHelper();
            mDBHandler = null;
        }
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        Timber.v("onDestroy");
        mMSHelper = null;
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        Timber.v("onDetach");
        super.onDetach();
    }

    // ----------- MusicServiceHelper.ReadyEvent

    public void onEvent(MusicServiceHelper.ReadyEvent event) {
        Timber.v("onReady");
        if (!checkMusicServiceHelper()) return;

        ArrayList<TrackInfo> trackHistory = mMSHelper.getTracksHistory();
        if (needRestoreUi &&
                trackHistory != null){
            fillPlaylist(trackHistory);
        }
    }

    // ----------- MusicPlayer.StateEvent

    public void onEvent(MusicPlayer.StateEvent event) {

        if (!checkMusicServiceHelper()) return;

        if (event.state == MusicPlayer.State.Playing) {
            Timber.v("onUpdate");
//            if (mAdapter != null) {
            fillPlaylist(mMSHelper.getTracksHistory());
//            }
        }
    }

    // ----------- List item

    /// Class is OnClickListener of ImageView 'star' -> onClick
    @Override
    public void onClick(View view) {
        Timber.v("onClick");
        if (view instanceof ImageView) {
            ImageView star = (ImageView) view;
            int position = mPlaylistView.getPositionForView(view);
            onTrackStarClicked(star, mAdapter.getItem(position));
        }
        else if (view.getTag() instanceof TrackViewHolder) {
            Timber.v("ListItem is clicked");
        }
    }

    /// Parameters
    // parent   :	The AdapterView where the click happened.
    // view     :	The view within the AdapterView that was clicked (this will be a view provided by the adapter)
    // position :	The position of the view in the adapter.
    // id       : 	The row id of the item that was clicked.
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Timber.v("onItemClick");
////        ImageView star = (ImageView) view.findViewById(R.id.p_item_trackStar);
//        ImageView star = (ImageView) view;
//        if (star != null) {
//            UserTrackInfo t = (UserTrackInfo) adapterView.getItemAtPosition(position);
//            onTrackStarClicked( star, t);
////            return;
//        }
    }

    private void onTrackStarClicked(ImageView star, DBTrackInfo uTrack) {
        Timber.v("onTrackStarClicked");
        try {
            if (star.getDrawable() == mStarOn) {
                // remove from favorites
                mREDao.deleteById(uTrack.id);
                star.setImageDrawable(mStarOff);
            } else if (star.getDrawable() == mStarOff) {
                // add as favorite:
                uTrack.isStarred = true;
                mREDao.create(uTrack);
                star.setImageDrawable(mStarOn);
            }
        } catch (RuntimeException e) {
            Timber.e("DB problem", e);
        }
    }


    // ----------- Other methods

    public void setHelper(MusicServiceHelper helper) {
        mMSHelper = helper;
    }

    public void setFavorite(boolean isFavorite, TrackInfo trackInfo) {
        try {
            if (isFavorite) {
                mREDao.deleteById(trackInfo.id);
            } else {
                mREDao.create(new DBTrackInfo(trackInfo));
            }
        } catch (RuntimeException e) {
            Timber.e("DB problem", e);
        }
        int count = mAdapter.getCount()-1;
        while (count >= 0) {
            DBTrackInfo uTrack = mAdapter.getItem(count);
            if (uTrack.id.compareTo(trackInfo.id)==0){
                uTrack.isStarred = isFavorite;
                mAdapter.notifyDataSetChanged();
                break;
            }
            count--;
        }
    }

    private boolean checkMusicServiceHelper()
    {
        if (mMSHelper == null) {
            Timber.v("mMSHelper is null");
            return false;
        }
        return true;
    }

    private void fillPlaylist(List<TrackInfo> tracks){
        Timber.v("fillPlaylist");

        List<DBTrackInfo> uTracks = new ArrayList<>();
        for (TrackInfo track : tracks) {
            DBTrackInfo uTrack = new DBTrackInfo(track);
            uTrack.isStarred = mREDao.idExists(track.id);
            uTracks.add(uTrack);
        }

        mAdapter.clear();
        mAdapter.addAll(uTracks);

    }

    // ----------- PlaylistAdapter

    private class PlaylistAdapter extends ArrayAdapter<DBTrackInfo> {

        LayoutInflater inflater = getActivity().getLayoutInflater();

        public PlaylistAdapter(Context context) {
            super(context, R.layout.playlist_item);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            TrackViewHolder h;

            if (null == convertView) {
                convertView = inflater.inflate(R.layout.playlist_item, null);
                h = new TrackViewHolder();
                h.title = (TextView) convertView.findViewById(R.id.p_item_trackTitle);
                h.tags = (TextView) convertView.findViewById(R.id.p_item_tags);
                h.duration = (TextView) convertView.findViewById(R.id.p_item_trackDuration);
                h.star = (ImageView) convertView.findViewById(R.id.p_item_trackStar);
                h.star.setOnClickListener(PlaylistFragment.this);
                convertView.setTag(h);
            } else {
                h = (TrackViewHolder) convertView.getTag();
            }

            DBTrackInfo t = getItem(position);
            h.title.setText(t.title);
            h.tags.setText(t.tags);
            h.duration.setText(MusicPlayer.getDuration(t.duration));
            h.star.setImageDrawable(t.isStarred ? mStarOn : mStarOff);

            return convertView;
        }

    }

    private static class TrackViewHolder {
        TextView title;
        TextView tags;
        TextView duration;
        ImageView star;
    }

    // -------------- Get DatabaseHelper
    private DatabaseHelper getHelper() {
        if (mDBHandler == null) {
            mDBHandler = OpenHelperManager.getHelper(getActivity(), DatabaseHelper.class);
        }
        return mDBHandler;
    }


}
