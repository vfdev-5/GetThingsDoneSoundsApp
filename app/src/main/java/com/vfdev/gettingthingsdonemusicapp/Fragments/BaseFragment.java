package com.vfdev.gettingthingsdonemusicapp.Fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.vfdev.gettingthingsdonemusicapp.DB.DBTrackInfo;
import com.vfdev.gettingthingsdonemusicapp.DB.DatabaseHelper;
import com.vfdev.gettingthingsdonemusicapp.Dialogs.TrackInfoDialog;
import com.vfdev.mimusicservicelib.MusicServiceHelper;
import com.vfdev.mimusicservicelib.core.TrackInfo;

import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 * Created by vfomin on 7/28/15.
 */
public class BaseFragment extends Fragment {


    // flag to restore ui at onReady() or onPause() methods
    protected boolean needRestoreUi=false;
    // DB handler
    protected DatabaseHelper mDBHandler;
    protected RuntimeExceptionDao<DBTrackInfo, String> mREDao;

    // Music Service
    protected MusicServiceHelper mMSHelper;

    // Toast dialog
    private Toast mToast;


    // ----------- Fragment methods

    public BaseFragment() {
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
        mMSHelper = MusicServiceHelper.getInstance();
        EventBus.getDefault().register(this);
        mDBHandler = getHelper();
        mREDao = getHelper().getTrackInfoREDao();
    }

    // onCreateView

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        Timber.v("onActivityCreated");
    }

    @Override
    public void onStart() {
        super.onStart();
        Timber.v("onStart");
        mToast = Toast.makeText(getActivity().getApplicationContext(), "", Toast.LENGTH_LONG);
    }

    // onResume

    @Override
    public void onPause() {
        Timber.v("onPause");
        super.onPause();
        needRestoreUi = true;
    }

    @Override
    public void onStop() {
        Timber.v("onStop");
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        Timber.v("onDestroy");
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        Timber.v("onDestroy");
        EventBus.getDefault().unregister(this);
        mMSHelper = null;
        if (mDBHandler != null) {
            OpenHelperManager.releaseHelper();
            mDBHandler = null;
        }
        mREDao = null;
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        Timber.v("onDetach");
        super.onDetach();
    }


    // ----------- Other methods

    protected void showMessage(String msg) {
        mToast.setText(msg);
        mToast.show();
    }


    protected void daoCreate(DBTrackInfo trackInfo) {
        try {
            mREDao.create(trackInfo);
            EventBus.getDefault().post(
                    new DBChangedEvent(DBChangedEvent.EVENT_ITEM_CREATED,
                            trackInfo.id, trackInfo)
            );
        } catch (RuntimeException e) {
            Timber.e("DB problem : Failed to create an item", e);
        }
    }

    protected void daoDeleteById(String trackId) {
        try {
            mREDao.deleteById(trackId);
            EventBus.getDefault().post(
                    new DBChangedEvent(DBChangedEvent.EVENT_ITEM_CREATED,
                            trackId, null)
            );
        } catch (RuntimeException e) {
            Timber.e("DB problem : Failed to delete an item by id", e);
        }
    }

    protected void openDialogOnTrack(TrackInfo trackInfo) {
        // Open Alert dialog :
        TrackInfoDialog dialog = new TrackInfoDialog(getActivity());
        dialog.setTrackInfo(trackInfo);
        dialog.show();
    }

    protected DatabaseHelper getHelper() {
        if (mDBHandler == null) {
            mDBHandler = OpenHelperManager.getHelper(getActivity(), DatabaseHelper.class);
        }
        return mDBHandler;
    }

    // ----------- BaseFragment.DBChangedEvent

    public class DBChangedEvent {
        public static final int EVENT_ITEM_CREATED = 0;
        public static final int EVENT_ITEM_DELETED = 1;
        public final int type;
        public final String id;
        public final DBTrackInfo item;
        public DBChangedEvent(int type, String id, DBTrackInfo item) {
            this.type = type;
            this.id = id;
            this.item = item;
        }
    }




}
