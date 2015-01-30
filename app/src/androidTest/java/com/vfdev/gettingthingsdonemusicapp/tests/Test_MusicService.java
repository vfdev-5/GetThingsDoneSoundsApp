package com.vfdev.gettingthingsdonemusicapp.tests;

import android.content.Context;
import android.content.Intent;
import android.test.ServiceTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.vfdev.gettingthingsdonemusicapp.MusicService;

/**
 * Created by vfomin on 1/29/15.
 */
public class Test_MusicService extends ServiceTestCase<MusicService> {

    Intent serviceStartIntent;

    public Test_MusicService() { super(MusicService.class); }


    @Override
    public void setUp() throws Exception {
        super.setUp();
        serviceStartIntent = new Intent(getContext(), MusicService.class);
    }

    @SmallTest
    public void testStartMusicService() throws Exception {

        // bind to the service
        bindService(serviceStartIntent);
        MusicService service = getService();

        // test something
        assertFalse(service.isPlaying());

        // start -> retrieve tracks
        startService(serviceStartIntent);

        //... wait while retrieving tracks



    }


}
