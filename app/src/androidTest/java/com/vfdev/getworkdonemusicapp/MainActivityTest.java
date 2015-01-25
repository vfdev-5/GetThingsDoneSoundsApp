package com.vfdev.getworkdonemusicapp;

import android.graphics.drawable.Drawable;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;
import android.widget.ImageButton;

import com.robotium.solo.Solo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 *
 * To Test : normal conditions
 *  - Initial layout
 *  - Service is started and bound
 *  - On Play button clicked music will be playing
 *  -
 *
 */

public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private Solo solo;
    private MainActivity activity;
    private MusicService service;
//    private ImageButton playPauseButton;

    // set accessible some methods and fields
    private Method methodExit;


    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        solo = new Solo(getInstrumentation(), getActivity());
        activity = getActivity();

        methodExit = MainActivity.class.getDeclaredMethod("exit");
        methodExit.setAccessible(true);


    }

    @Override
    public void tearDown() throws Exception {
        try {
            solo.finalize();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        methodExit.invoke(activity);
        super.tearDown();
    }

    @SmallTest
    public void testNormalUsage_1() throws Exception {
        // Verify initial activity state:
        assertNotNull(solo.getView(R.id.playPauseButton));
//        playPauseButton = (ImageButton) solo.getView(R.id.playPauseButton);
        assertNotNull(solo.getView(R.id.nextTrack));
        assertNotNull(solo.getView(R.id.prevTrack));
        assertNotNull(solo.getView(R.id.trackDuration));
        assertNotNull(solo.getView(R.id.trackTitle));
        assertNotNull(solo.getView(R.id.waveform));

        // Assert that MusicService is started
        Field fService = MainActivity.class.getDeclaredField("mService");
        fService.setAccessible(true);
        service = (MusicService) fService.get(activity);
        assertNotNull(service);

        // wait until the end of retrieving operation
        solo.waitForDialogToClose();

        // verify if trackid list is not empty
        Field fTracks = MusicService.class.getDeclaredField("mTracks");
        fTracks.setAccessible(true);
        assertEquals(false, ((ArrayList<String>) fTracks.get(service)).isEmpty());

        // start player
        solo.clickOnButton(R.id.playPauseButton);

//        assertEquals(pauseDrawable, playPauseButton.getDrawable());




    }



}
