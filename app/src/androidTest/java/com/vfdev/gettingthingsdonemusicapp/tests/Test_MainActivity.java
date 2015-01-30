package com.vfdev.gettingthingsdonemusicapp.tests;

import android.content.Intent;
import android.test.ActivityUnitTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.View;
import android.widget.TextView;

import com.robotium.solo.Solo;
import com.vfdev.gettingthingsdonemusicapp.MainActivity;
import com.vfdev.gettingthingsdonemusicapp.R;


/**
 * Isolated test of Main Activity
 */
public class Test_MainActivity extends ActivityUnitTestCase<MainActivity> {

    private Solo solo;
    private Intent mainActivityIntent;

    public Test_MainActivity() { super(MainActivity.class);  }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mainActivityIntent = new Intent(getInstrumentation().getTargetContext(),
                MainActivity.class);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }


    @SmallTest
    public void testInitialLayout() throws Exception {
        // start activity :
        startActivity(mainActivityIntent, null, null);
        solo = new Solo(getInstrumentation(), getActivity());

        // Verify initial activity state:
        assertNotNull(solo.getView(R.id.playPauseButton));

        assertNotNull(solo.getView(R.id.nextTrack));
        TextView nextTrackButton = (TextView) solo.getView(R.id.nextTrack);
        assertEquals(true, nextTrackButton.getVisibility() == View.INVISIBLE);

        assertNotNull(solo.getView(R.id.prevTrack));
        TextView prevTrackButton = (TextView) solo.getView(R.id.prevTrack);
        assertEquals(true, prevTrackButton.getVisibility() == View.INVISIBLE);

        assertNotNull(solo.getView(R.id.trackDuration));
        assertNotNull(solo.getView(R.id.trackTitle));
        assertNotNull(solo.getView(R.id.waveform));

    }




}
