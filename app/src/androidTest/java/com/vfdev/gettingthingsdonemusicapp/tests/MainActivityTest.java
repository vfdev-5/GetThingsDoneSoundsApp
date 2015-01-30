package com.vfdev.gettingthingsdonemusicapp;

import android.graphics.drawable.Drawable;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.robotium.solo.Condition;
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
    private ImageButton playPauseButton;
    private Drawable playDrawable;
    private Drawable pauseDrawable;

    private TextView nextTrackButton;
    private TextView prevTrackButton;


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

        Field fPlayDrawable = MainActivity.class.getDeclaredField("mPlayButtonDrawable");
        fPlayDrawable.setAccessible(true);
        playDrawable = (Drawable) fPlayDrawable.get(activity);
        assertNotNull(playDrawable);

        Field fPauseDrawable = MainActivity.class.getDeclaredField("mPauseButtonDrawable");
        fPauseDrawable.setAccessible(true);
        pauseDrawable = (Drawable) fPauseDrawable.get(activity);
        assertNotNull(pauseDrawable);

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
    public void testInitialLayout() throws Exception {
        // Verify initial activity state:
        assertNotNull(solo.getView(R.id.playPauseButton));
        playPauseButton = (ImageButton) solo.getView(R.id.playPauseButton);

        assertEquals(playPauseButton.getDrawable(), playDrawable);

        assertNotNull(solo.getView(R.id.nextTrack));
        nextTrackButton = (TextView) solo.getView(R.id.nextTrack);
        assertEquals(true, nextTrackButton.getVisibility() == View.INVISIBLE);

        assertNotNull(solo.getView(R.id.prevTrack));
        prevTrackButton = (TextView) solo.getView(R.id.prevTrack);
        assertEquals(true, prevTrackButton.getVisibility() == View.INVISIBLE);

        assertNotNull(solo.getView(R.id.trackDuration));
        assertNotNull(solo.getView(R.id.trackTitle));
        assertNotNull(solo.getView(R.id.waveform));
    }

    @SmallTest
    public void testUiLogic() throws Exception {

        // wait until the end of retrieving operation
        solo.waitForDialogToClose();
        solo.sleep(1000);
    }



    @SmallTest
    public void testNormalUsage_1() throws Exception {
        // Verify initial activity state:
        assertNotNull(solo.getView(R.id.playPauseButton));
        playPauseButton = (ImageButton) solo.getView(R.id.playPauseButton);

        assertEquals(playPauseButton.getDrawable(), playDrawable);

        assertNotNull(solo.getView(R.id.nextTrack));
        nextTrackButton = (TextView) solo.getView(R.id.nextTrack);
        assertEquals(true, nextTrackButton.getVisibility() == View.INVISIBLE);

        assertNotNull(solo.getView(R.id.prevTrack));
        prevTrackButton = (TextView) solo.getView(R.id.prevTrack);
        assertEquals(true, prevTrackButton.getVisibility() == View.INVISIBLE);

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
        // wait until dialog is really closed
        solo.sleep(1000);
        solo.clickOnView(playPauseButton);
        // wait until service.play() is executed
        solo.sleep(2000);
        // check if buttons is changed
        assertEquals(pauseDrawable, playPauseButton.getDrawable());

        // sleeps until track is loaded
        solo.waitForCondition(new IsVisibleCondition(solo.getView(R.id.trackDuration)), 5000);

        // check waveform, title, duration and next track are visible
        assertTrue(solo.getView(R.id.waveform).getVisibility() == View.VISIBLE);
        assertTrue(solo.getView(R.id.trackTitle).getVisibility()==View.VISIBLE);
        assertTrue(solo.getView(R.id.trackDuration).getVisibility()==View.VISIBLE);
        assertTrue(solo.getView(R.id.nextTrack).getVisibility()==View.VISIBLE);

        // check if service is playing;
        assertTrue(service.isPlaying());

        // click on pause:
        solo.clickOnView(playPauseButton);
        solo.sleep(500);
        // check if buttons is changed
        assertEquals(playDrawable, playPauseButton.getDrawable());
        // check that service is not playing
        assertFalse(service.isPlaying());

        // change track :



        // test sleep
        solo.sleep(5000);



    }




    private class IsVisibleCondition implements Condition {
        private View mView;
        IsVisibleCondition(View view) {
            mView = view;
        }
        @Override
        public boolean isSatisfied() {
            return mView.getVisibility() == View.VISIBLE;
        }
    }


}
