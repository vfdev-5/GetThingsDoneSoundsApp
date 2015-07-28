package com.vfdev.gettingthingsdonemusicapp.DB;

import com.j256.ormlite.field.DatabaseField;
import com.vfdev.mimusicservicelib.core.TrackInfo;

/**
 * Created by vfomin on 7/28/15.
 */
public class DBTrackInfo  {

    @DatabaseField(id = true)
    public String id;

    @DatabaseField
    public String title;

    @DatabaseField
    public int duration;

    @DatabaseField
    public String tags;

    @DatabaseField
    public String streamUrl;

    @DatabaseField
    public String waveformUrl;

    @DatabaseField
    public boolean isStarred = false;


    public TrackInfo trackInfo;

    public DBTrackInfo() {
        // ORMLite needs a no-arg constructor
    }

    public DBTrackInfo(TrackInfo trackInfo) {
        this.trackInfo = trackInfo;
        this.id = trackInfo.id;
        this.title = trackInfo.title;
        this.duration = trackInfo.duration;
        this.tags = trackInfo.tags;
        this.streamUrl = trackInfo.streamUrl;
        this.waveformUrl = trackInfo.fullInfo.containsKey("waveform_url") ?
                trackInfo.fullInfo.get("waveform_url") : "";
    }


}
