package com.vfdev.gettingthingsdonemusicapp.Fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
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
import com.vfdev.gettingthingsdonemusicapp.Dialogs.TrackInfoDialog;
import com.vfdev.gettingthingsdonemusicapp.R;
import com.vfdev.mimusicservicelib.MusicService;
import com.vfdev.mimusicservicelib.MusicServiceHelper;
import com.vfdev.mimusicservicelib.core.MusicPlayer;
import com.vfdev.mimusicservicelib.core.TrackInfo;

/**
 * Playlist fragment
 */
public class PlaylistFragment extends BaseFragment implements
        ListView.OnItemClickListener,
        View.OnClickListener
{

    // UI
    @InjectView(R.id.playlist)
    protected ListView mPlaylistView;

    private Drawable mStarOn;
    private Drawable mStarOff;

    // Playlist
    private PlaylistAdapter mAdapter;


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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Timber.v("onCreateView");

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
    public void onResume(){
        Timber.v("onResume");
        super.onResume();

        // restore UI state:
        ArrayList<TrackInfo> trackHistory = mMSHelper.getTracksHistory();
        if (needRestoreUi &&
                trackHistory != null) {
            fillPlaylist(trackHistory);
        }
    }

    // ----------- MusicServiceHelper.ReadyEvent

    public void onEvent(MusicServiceHelper.ReadyEvent event) {
        Timber.v("onReady");

        ArrayList<TrackInfo> trackHistory = mMSHelper.getTracksHistory();
        if (needRestoreUi &&
                trackHistory != null){
            fillPlaylist(trackHistory);
        }
    }

    // ----------- MusicPlayer.StateEvent

    public void onEvent(MusicPlayer.StateEvent event) {

        if (event.state == MusicPlayer.State.Playing) {
            Timber.v("onUpdate");
            fillPlaylist(mMSHelper.getTracksHistory());
        }
    }

    // ----------- DBChangedEvent

    public void onEvent(DBChangedEvent event) {
        if (event.type == DBChangedEvent.EVENT_ITEM_CREATED ||
                event.type == DBChangedEvent.EVENT_ITEM_DELETED) {
            mAdapter.notifyDataSetChanged();
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

        TextView title = (TextView) view.findViewById(R.id.p_item_trackTitle);
        if (title != null) {
            openDialogOnTrack(mAdapter.getItem(position), TrackInfoDialog.PLAY_BUTTON);
        }

    }

    private void onTrackStarClicked(ImageView star, TrackInfo uTrack) {
        Timber.v("onTrackStarClicked");
        if (star.getDrawable() == mStarOn) {
            // remove from favorites
            daoDeleteById(uTrack.id);
            star.setImageDrawable(mStarOff);
        } else if (star.getDrawable() == mStarOff) {
            // add as favorite:
            daoCreate(new DBTrackInfo(uTrack));
            star.setImageDrawable(mStarOn);
        }
    }

    // ----------- Other methods

    private void fillPlaylist(List<TrackInfo> tracks){
        Timber.v("fillPlaylist");

        mAdapter.clear();
        mAdapter.addAll(tracks);

    }

    // ----------- PlaylistAdapter

    private class PlaylistAdapter extends ArrayAdapter<TrackInfo> {

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

            TrackInfo t = getItem(position);
            h.title.setText(t.title);
            h.tags.setText(t.tags.isEmpty() ? getString(R.string.trackinfo_dialog_notags) : t.tags);
            h.duration.setText(MusicPlayer.getDuration(t.duration));
            h.star.setImageDrawable(mREDao.idExists(t.id) ? mStarOn : mStarOff);

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
