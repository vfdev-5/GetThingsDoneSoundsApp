package com.vfdev.gettingthingsdonemusicapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.vfdev.gettingthingsdonemusicapp.core.AppDBHandler;
import com.vfdev.gettingthingsdonemusicapp.core.SoundCloudHelper;

import java.util.ArrayList;

import butterknife.ButterKnife;
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
    2.1) OK = Add local DB to store conf:
    2.2) Download playing track
    2.3) PlaylistFragment : tracklist : item = { track name/tags ; duration ; star }
        a) Item onClick : play track -> remove all track after
    2.4) Track Title onClick : AlertDialog : {Mark as favorite; Download to phone; Open in SoundCloud}


 2) Abnormal usage
    2.1) No network

 3) BUGS :

    1) play track -> next -> back -> restore from notification

 */

public class MainActivity extends Activity implements
        MusicService.OnErrorListener,
        ServiceConnection,
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



    // Service connection
    private MusicService mService;
    private boolean mBound = false;

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

        setupPagerUI();

//        mProgressDialog = new ProgressDialog(this);
        mToast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_LONG);

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

    }

    @Override
    protected void onResume(){
        Timber.v("onResume");
        super.onResume();

    }

    @Override
    protected void onDestroy() {
        Timber.v("onDestroy");

        // Unbind from the service
        if (mBound) {
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


    // ----------- Service connection

    // Defines callbacks for service binding, passed to bindService()
    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
        Timber.v("Main activity is connected to MusicService");

        // We've bound to MusicService, cast the IBinder and get LocalService instance
        MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
        mService = binder.getService();
        mBound = true;
        mService.setErrorListener(this);
        mMainFragment.setService(mService);
        mPlaylistFragment.setService(mService);
    }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
        Timber.v("Main activity is disconnected from MusicService");
        mBound = false;
        mMainFragment.setService(null);
    }

    // --------- Music Service onError listener

    @Override
    public void onShowErrorMessage(String msg) {
        Timber.v("onShowErrorMessage");
        showMessage(msg);
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

    private void setupDB() {
        Timber.v("Setup DB");
        mDBHandler = new AppDBHandler(this);

        SoundCloudHelper.getInstance().setTags(mDBHandler.getTags());

    }

    private void startMusicService() {
        // start music service -> service is independent
        startService(new Intent(this, MusicService.class));
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

    private void settings() {

        SettingsDialog dialog = new SettingsDialog(this);
        dialog.setData(mDBHandler.getTags());
        dialog.show();

    }

    private void setupNewTags(String tags) {

        if (tags.isEmpty()) return;

        mDBHandler.setTags(tags);
        Toast.makeText(this, getString(R.string.tags_updated), Toast.LENGTH_SHORT).show();

        // start retrieving tracks for new tags
        SoundCloudHelper.getInstance().setTags(mDBHandler.getTags());

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