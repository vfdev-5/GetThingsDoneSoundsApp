package com.vfdev.gettingthingsdonemusicapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import timber.log.Timber;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.vfdev.gettingthingsdonemusicapp.DB.DBTrackInfo;
import com.vfdev.gettingthingsdonemusicapp.Fragments.FavoriteTracksFragment;
import com.vfdev.gettingthingsdonemusicapp.Fragments.MainFragment;
import com.vfdev.gettingthingsdonemusicapp.Fragments.PlaylistFragment;
import com.vfdev.gettingthingsdonemusicapp.Dialogs.SettingsDialog;
import com.vfdev.mimusicservicelib.MusicService;
import com.vfdev.mimusicservicelib.MusicServiceHelper;
import com.vfdev.mimusicservicelib.core.MusicPlayer;
import com.vfdev.mimusicservicelib.core.SoundCloundProvider;
import com.vfdev.mimusicservicelib.core.TrackInfo;

import de.greenrobot.event.EventBus;


public class MainActivity extends Activity implements
        SettingsDialog.SettingsDialogCallback
{

    // UI
    private ProgressDialog mProgressDialog;
    // Pager View/Adapter
    private ViewPager mViewPager;
    private PagerAdapter mPagerAdapter;
    // Fragments
    private MainFragment mMainFragment;
    private PlaylistFragment mPlaylistFragment;
    private FavoriteTracksFragment mFavoriteTracksFragment;

    // MusicService helper
    private MusicServiceHelper mMSHelper;

    // Toast dialog
    private Toast mToast;

    // ------- Activity methods

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timber.v("onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        // set custom title :
        setTitle(R.string.main_activity_name);

        // setup MusicServiceHelper
        mMSHelper = MusicServiceHelper.getInstance().init(this, new SoundCloundProvider(), MainActivity.class);
        mMSHelper.startMusicService();

        // setup UIL
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this).build();
        ImageLoader.getInstance().init(config);


        // initialize some fragments (uses mMSHelper)
        setupPagerUI();

        mProgressDialog = new ProgressDialog(this);
        mToast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_LONG);

        EventBus.getDefault().register(this);

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

    }

    @Override
    protected void onResume(){
        Timber.v("onResume");
        super.onResume();

    }

    @Override
    protected void onDestroy() {
        Timber.v("onDestroy");

        mMSHelper.release();
        ImageLoader.getInstance().destroy();
        EventBus.getDefault().unregister(this);
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

    // ----------- MusicServiceHelper.ReadyEvent

    public void onEvent(MusicServiceHelper.ReadyEvent event) {
        ArrayList<TrackInfo> playlist = mMSHelper.getPlaylist();
        if (playlist.isEmpty()) {
            // get tags from settings :
            String query = getTags();
            Timber.v("onReady -> setupTracks : " + query);
            mMSHelper.setupTracks(query);
        }
    }

    // --------- MusicPlayer.ErrorEvent & MusicService.ErrorEvent

    public void onEvent(MusicPlayer.ErrorEvent event) {
        Timber.v("onEvent : MusicPlayer.ErrorEvent : event.code=" + event.code);
        if (event.code == MusicPlayer.ERROR_DATASOURCE ||
                event.code == MusicPlayer.ERROR_APP ||
                event.code == MusicPlayer.ERROR_NO_AUDIOFOCUS) {
            Toast.makeText(this, R.string.app_err, Toast.LENGTH_SHORT).show();
        }
    }

    public void onEvent(MusicService.ErrorEvent event) {
        Timber.v("onEvent : MusicService.ErrorEvent : event.code=" + event.code);
        if (event.code == MusicService.APP_ERR) {
            Toast.makeText(this, "Ops, there is an application error", Toast.LENGTH_SHORT).show();
        } else if (event.code == MusicService.NOTRACKS_ERR) {
            Toast.makeText(this, "No tracks found", Toast.LENGTH_SHORT).show();
        } else if (event.code == MusicService.CONNECTION_ERR) {
            Toast.makeText(this, "Ops, internet connection problem", Toast.LENGTH_SHORT).show();
        } else if (event.code == MusicService.QUERY_ERR) {
            Toast.makeText(this, "There is a problem with your query", Toast.LENGTH_SHORT).show();
        }
    }

    // --------- FavoriteTracksFragment.OnPlayFavoriteTracksListener

//    @Override
    public void onPlay(List<DBTrackInfo> tracks) {
        Timber.v("onPlay");
        if (mMSHelper.getPlayer() != null) {
            mMSHelper.getPlayer().clearTracks();
            for (DBTrackInfo track : tracks) {
                mMSHelper.getPlayer().addTrack(track.trackInfo);
            }
        }
    }


    // --------- Other class methods

    private void showMessage(String msg) {
        mToast.setText(msg);
        mToast.show();
    }

    private void startProgressDialog(String msg) {
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage(msg);
        mProgressDialog.show();
    }

    private void setupPagerUI() {
        Timber.v("setupPagerUI");

        // Create fragments:
        mMainFragment = new MainFragment();
        mPlaylistFragment = new PlaylistFragment();
        mFavoriteTracksFragment = new FavoriteTracksFragment();

        // Create pager adapter
        mPagerAdapter = new PagerAdapter(getFragmentManager());
        mPagerAdapter.appendFragment(mMainFragment);
        mPagerAdapter.appendFragment(mPlaylistFragment);
        mPagerAdapter.appendFragment(mFavoriteTracksFragment);

        // Create and set up the ViewPager .
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mPagerAdapter);

        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                Timber.v("onPageSelected : " + String.valueOf(position));
            }
        });

    }

    private void exit() {
        Timber.v("exit");
        mMSHelper.stopMusicService();
        finish();
    }

    private void about() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.about_dialog_title)
                .setView(getLayoutInflater().inflate(R.layout.alertdialog_about, null));
        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private void settings() {
        SettingsDialog dialog = new SettingsDialog(this);
        dialog.setData(getTags());
        dialog.show();

    }

    private String getTags() {
        SharedPreferences prefs = getSharedPreferences("Tags",0);
        return prefs.getString("Tags", getString(R.string.settings_default_tags));
    }

    private void writeTags(String tags) {
        Timber.v("writeTags : " + tags);
        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences prefs = getSharedPreferences("Tags", 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("Tags", tags);
        // Commit the edits!
        editor.commit();
    }

    private void setupNewTags(String tags) {

        if (tags.isEmpty()) return;

        writeTags(tags);
        Toast.makeText(this, getString(R.string.tags_updated), Toast.LENGTH_SHORT).show();

        // start retrieving tracks for new tags
        mMSHelper.clearPlaylist();
        mMSHelper.setupTracks(tags);
    }

    // ----------- SettingsDialog.SettingsDialogCallback

    @Override
    public void onUpdateData(String newTags) {
        setupNewTags(newTags);
    }

    @Override
    public void onResetDefault() {
        setupNewTags(getString(R.string.settings_default_tags));
    }

    // ------------ PagerAdapter

    private class PagerAdapter extends FragmentPagerAdapter {

        private ArrayList<Fragment> mFragments;

        public PagerAdapter(FragmentManager fm) {
            super(fm);
            // Setup fragments:
            mFragments = new ArrayList<Fragment>();
        }

        public void appendFragment(Fragment f) {
            mFragments.add(f);
        }


        @Override
        public Fragment getItem(int position) {

            if (position >=0 && position < mFragments.size()) {
                return mFragments.get(position);
            }
            return null;
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "Fragment Title";
        }

    }


}