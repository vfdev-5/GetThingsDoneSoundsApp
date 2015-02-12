package com.vfdev.gettingthingsdonemusicapp.core;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import timber.log.Timber;

/*
   *  Helper class to retrieve track info from Soundcloud
   *
   *  Example of request :
   *  {
   *  "kind":"track",
   *  "id":3207,
   *  "created_at":"2008/03/04 01:11:02 +0000",
   *  "user_id":1656,
   *  "duration":492800,
   *  "commentable":true,
   *  "state":"finished",
   *  "original_content_size":15872024,
   *  "last_modified":"2011/07/05 15:56:58 +0000",
   *  "sharing":"public",
   *  "tag_list":"\"qburns qburnsabstractmessage redeye doubledown housemusic\"",
   *  "permalink":"party-as-a-verb",
   *  "streamable":true,
   *  "embeddable_by":"all",
   *  "downloadable":false,
   *  "purchase_url":"http://www.stompy.com/catalog/quick-search-results.php?textfield=party+as+a+verb&submit=submit",
   *  "label_id":null,
   *  "purchase_title":null,
   *  "genre":"House",
   *  "title":"Party As A Verb",
   *  "description":"This is Q-Burns Abstract Message in collaboration with Dallas DJ and booty-poet Red Eye. Released on Doubledown Recordings in digital and vinyl formats. It's a club popper!",
   *  "label_name":"Doubledown",
   *  "release":"",
   *  "track_type":"original",
   *  "key_signature":"",
   *  "isrc":null,
   *  "video_url":null,
   *  "bpm":null,
   *  "release_year":null,
   *  "release_month":null,
   *  "release_day":null,
   *  "original_format":"mp3",
   *  "license":"all-rights-reserved",
   *  "uri":"https://api.soundcloud.com/tracks/3207",
   *  "user":{
   *        "id":1656,
   *        "kind":"user",
   *        "permalink":"q-burns-abstract-message",
   *        "username":"Q-Burns Abstract Message",
   *        "last_modified":"2015/01/25 19:38:39 +0000",
   *        "uri":"https://api.soundcloud.com/users/1656",
   *        "permalink_url":"http://soundcloud.com/q-burns-abstract-message",
   *        "avatar_url":"https://i1.sndcdn.com/avatars-000081857164-czaoc6-large.jpg"
   *        },
   *   "permalink_url":"http://soundcloud.com/q-burns-abstract-message/party-as-a-verb",
   *   "artwork_url":null,
   *   "waveform_url":"https://w1.sndcdn.com/YSzOKu308iA3_m.png",
   *   "stream_url":"https://api.soundcloud.com/tracks/3207/stream",
   *   "playback_count":372,
   *   "download_count":0,
   *   "favoritings_count":2,
   *   "comment_count":2,
   *   "attachments_uri":"https://api.soundcloud.com/tracks/3207/attachments",
   *   "policy":"ALLOW"
   *   }
   *
   *
 */

public class SoundCloudHelper {

    // Connection
    private OkHttpClient mSoundCloudClient;
    private String CLIENT_ID;

    private final static int HTTP_OK=200;
    private final static String API_URL="http://api.soundcloud.com/";
    private final static String REQUEST_TRACKS_URL=API_URL+"tracks.json?genres=";
    private final static String REQUEST_TRACKS_URL_WITH_TAGS=API_URL+"tracks.json?tags=";
    private final static String REQUEST_A_TRACK_URL=API_URL+"tracks/";
    private final static int TRACKS_LIMIT=100;

    // REQUEST ERRORS
    public final static int APP_ERR=1;
    public final static int CONNECTION_ERR=2;

    // Track info:
    private String[] mStyles = new String[] {"electro", "trance"};
    private String mTags = "";

    // Workers & Listeners
    DownloadTrackInfoAsyncTask mTrackInfoDownloader;
    OnDownloadTrackIdsListener mTrackIdsListener;
    OnDownloadTrackInfoListener mTrackInfoListener;

    // ----- Class methods

    private static SoundCloudHelper mInstance = new SoundCloudHelper();


    public static SoundCloudHelper getInstance() {
        return mInstance;
    }

    public SoundCloudHelper() {

        CLIENT_ID="1abbcf4f4c91b04bb5591fe5a9f60821";
        mSoundCloudClient = new OkHttpClient();
    }

    public void setTags(String tags) {
        Timber.v("setTags : " + tags);

        StringBuilder out= new StringBuilder();
        for (String s : tags.split(",")) {
            String tag=s.toLowerCase();
            tag = tag.replaceAll("^\\s+", "").replaceAll("\\s+$","").replaceAll("\\s","%20");
            out.append(tag).append(",");
        }
        out.deleteCharAt(out.length()-1);
        mTags = out.toString();
    }

    public void setStyles(String[] styles) {
        mStyles = styles;
    }

    public String getCompleteStreamUrl(String trackStreamUrl) {
        return trackStreamUrl + "?client_id=" + CLIENT_ID;
    }

    public void setOnDownloadTrackIdsListener(OnDownloadTrackIdsListener listener) {
        mTrackIdsListener = listener;
    }

    public void setOnDownloadTrackInfoListener(OnDownloadTrackInfoListener listener) {
        mTrackInfoListener = listener;
    }

//    public void downloadTrackIdsUsingStyles() {
//    }

    public void downloadTrackInfoUsingTags() {

        if (mTrackInfoDownloader == null) {
            mTrackInfoDownloader = new DownloadTrackInfoAsyncTask(mTrackInfoListener);
            mTrackInfoDownloader.execute();
        }
    }


    // ---------- DownloadTrackIdsRunnable using styles

    private class DownloadTrackIdsRunnable implements Runnable {

        OnDownloadTrackIdsListener mListener;

        DownloadTrackIdsRunnable(OnDownloadTrackIdsListener listener) {
            mListener = listener;
        }

        ArrayList<TrackInfo> mTracks;

        Handler mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
            if (mListener != null)
                mListener.onDownloadTrackIds(message.getData(), mTracks);
            }
        };

        @Override
        public void run() {

            Message m = new Message();
            Bundle result = new Bundle();

            for (String style : mStyles) {
                Timber.v("DownloadTrackIds : request track of genre : " + style);

                String requestUrl = REQUEST_TRACKS_URL;
                requestUrl += style;
                requestUrl += "&limit=" + TRACKS_LIMIT;
                requestUrl += "&offset=" + String.valueOf(new Random().nextInt(1000));
                requestUrl += "&client_id=" + CLIENT_ID;

                try {
                    Request request = new Request.Builder()
                            .url(requestUrl).build();

                    Response response = mSoundCloudClient.newCall(request).execute();
                    int code = response.code();
                    String responseStr = response.body().string();
                    if (code == HTTP_OK) {
                        // Parse the response:
                        JSONArray tracksJSON = new JSONArray(responseStr);
                        Timber.v("getTracksInBackground : found " + tracksJSON.length() + " tracks");

                        mTracks = new ArrayList<TrackInfo>();

                        for (int i = 0; i < tracksJSON.length(); i++) {
                            JSONObject trackJSON = tracksJSON.getJSONObject(i);
                            if (trackJSON.getBoolean("streamable")) {
                                TrackInfo tInfo = new TrackInfo();
                                tInfo.id = trackJSON.getString("id");
                                tInfo.title = trackJSON.getString("title");
                                tInfo.duration = trackJSON.getInt("duration");
                                tInfo.tags = trackJSON.getString("tag_list");
                                tInfo.streamUrl = trackJSON.getString("stream_url");
                                tInfo.soundcloudUrl = trackJSON.getString("permalink_url");
                                if (trackJSON.getBoolean("downloadable")) {
                                    tInfo.downloadUrl = trackJSON.getString("download_url");
                                }
                                tInfo.waveformUrl=trackJSON.getString("waveform_url");
                                mTracks.add(tInfo);
                            }
                        }
                    } else {
                        Timber.e("getTracksInBackground : request error : " + responseStr);

                        result.putBoolean("Result", false);
                        result.putInt("ErrorType", APP_ERR);
                        m.setData(result);
                        mHandler.handleMessage(m);
//                        if (mListener != null)
//                            mListener.onDownloadTrackIds(result, null);
                        return;
                    }
                } catch (IOException e) {
                    Timber.i(e, "getTracksInBackground : SoundCloud get request error : " + e.getMessage());
                    result.putBoolean("Result", false);
                    result.putInt("ErrorType", CONNECTION_ERR);
                    m.setData(result);
                    mHandler.handleMessage(m);
//                    if (mListener != null)
//                        mListener.onDownloadTrackIds(result, null);
                    return;
                } catch (JSONException e) {
//                    e.printStackTrace();
                    Timber.e(e, "getTracksInBackground : JSON parse error : " + e.getMessage());
                    result.putBoolean("Result", false);
                    result.putInt("ErrorType", APP_ERR);
                    m.setData(result);
                    mHandler.handleMessage(m);
//                    if (mListener != null)
//                        mListener.onDownloadTrackIds(result, null);
                    return;
                }
            }
            result.putBoolean("Result", true);
            m.setData(result);
            mHandler.handleMessage(m);
//            if (mListener != null)
//                mListener.onDownloadTrackIds(result, tracks);
        }
    }

    public static interface OnDownloadTrackIdsListener {
        public void onDownloadTrackIds(Bundle result, ArrayList<TrackInfo> trackIds);
    }

    // ---------- DownloadTrackInfoRunnable using tags

    private class DownloadTrackInfoAsyncTask extends AsyncTask<Void, Void, Bundle> {

        OnDownloadTrackInfoListener mListener;
        ArrayList<TrackInfo> tracks = new ArrayList<TrackInfo>();

        DownloadTrackInfoAsyncTask(OnDownloadTrackInfoListener listener) {
            mListener = listener;
        }

        protected Bundle doInBackground(Void... params){
            Bundle result = new Bundle();
            Timber.v("DownloadTrackIds : request tracks on tags : " + mTags);

            // Loop several times to find a track
            int limit=5;
            int count = 5;
            while (count > 0) {

                String requestUrl = REQUEST_TRACKS_URL_WITH_TAGS;
                requestUrl += mTags;
                requestUrl += "&limit=" + String.valueOf(limit);
                if (count > 1) {
                    requestUrl += "&offset=" + String.valueOf(new Random().nextInt(1000));
                }
                requestUrl += "&client_id=" + CLIENT_ID;

                try {
                    Request request = new Request.Builder()
                            .url(requestUrl).build();

                    Response response = mSoundCloudClient.newCall(request).execute();
                    int code = response.code();
                    String responseStr = response.body().string();
                    if (code == HTTP_OK) {
                        // Parse the response:
                        JSONArray tracksJSON = new JSONArray(responseStr);
                        int length = tracksJSON.length();


                        if (length == 0) {
                            count--;
                            continue;
                        }

                        for (int i=0;i<length;i++) {
                            JSONObject trackJSON = tracksJSON.getJSONObject(i);
                            if (trackJSON.getBoolean("streamable")) {

                                TrackInfo tInfo = new TrackInfo();
                                tInfo.id = trackJSON.getString("id");
                                tInfo.title = trackJSON.getString("title");
                                tInfo.duration = trackJSON.getInt("duration");
                                tInfo.tags = trackJSON.getString("tag_list");
                                tInfo.streamUrl = trackJSON.getString("stream_url");
                                tInfo.soundcloudUrl = trackJSON.getString("permalink_url");
                                if (trackJSON.getBoolean("downloadable")) {
                                    tInfo.downloadUrl = trackJSON.getString("download_url");
                                }
                                tInfo.waveformUrl=trackJSON.getString("waveform_url");
                                tracks.add(tInfo);
                            }

                        }
                        if (!tracks.isEmpty()) {
                            Timber.v("getTracksInBackground : found " + tracks.size() + " tracks");
                            result.putBoolean("Result", true);
                            return result;
                        }
                    } else {
                        Timber.e("getTracksInBackground : request error : " + responseStr);

                        result.putBoolean("Result", false);
                        result.putInt("ErrorType", APP_ERR);
                        return result;
                    }
                } catch (IOException e) {
                    Timber.i(e, "getTracksInBackground : SoundCloud get request error : " + e.getMessage());
                    result.putBoolean("Result", false);
                    result.putInt("ErrorType", CONNECTION_ERR);
                    return result;
                } catch (JSONException e) {
                    Timber.e(e, "getTracksInBackground : JSON parse error : " + e.getMessage());
                    result.putBoolean("Result", false);
                    result.putInt("ErrorType", APP_ERR);
                    return result;
                }
                count--;
            }
            return result;
        }

        protected void onPostExecute(Bundle result) {
            if (mListener != null) {
                mListener.onDownloadTrackInfo(result, tracks);
            }
            mTrackInfoDownloader = null;
        }

    }

    public static interface OnDownloadTrackInfoListener {
        public void onDownloadTrackInfo(Bundle result, ArrayList<TrackInfo> trackInfo);
    }


    // ---------- DownloadTrackIds AsyncTask
    /*
    private class DownloadTrackIds extends AsyncTask<Void, Void, Bundle> {
        protected Bundle doInBackground(Void... params) {

            Bundle result = new Bundle();
            for (String style : mStyles) {
                Timber.v("DownloadTrackIds : request track of genre : " + style);

                String requestUrl = REQUEST_TRACKS_URL;
                requestUrl += style;
                requestUrl += "&limit=" + TRACKS_LIMIT;
                requestUrl += "&offset=" + String.valueOf(new Random().nextInt(1000));
                requestUrl += "&client_id=" + CLIENT_ID;

                try {
                    Request request = new Request.Builder()
                            .url(requestUrl).build();

                    Response response = mSoundCloudClient.newCall(request).execute();
                    int code = response.code();
                    String responseStr = response.body().string();
                    if (code == HTTP_OK) {
                        // Parse the response:
                        JSONArray tracksJSON = new JSONArray(responseStr);
                        Timber.v("getTracksInBackground : found " + tracksJSON.length() + " tracks");

                        for (int i = 0; i < tracksJSON.length(); i++) {
                            JSONObject trackJSON = tracksJSON.getJSONObject(i);
                            if (trackJSON.getBoolean("streamable")) {
                                String id = trackJSON.getString("id");
                                mTracks.add(id);
                            }
                        }
                    } else {
                        Timber.e("getTracksInBackground : request error : " + responseStr);

                        result.putBoolean("Result", false);
                        result.putString("Message", getString(R.string.app_err));
                        return result;
                    }
                } catch (IOException e) {
                    Timber.i(e, "getTracksInBackground : SoundCloud get request error : " + e.getMessage());
                    result.putBoolean("Result", false);
                    result.putString("Message", getString(R.string.connx_err));
                    return result;
                } catch (JSONException e) {
//                    e.printStackTrace();
                    Timber.e(e, "getTracksInBackground : JSON parse error : " + e.getMessage());
                    result.putBoolean("Result", false);
                    result.putString("Message", getString(R.string.app_err));
                    return result;
                }
            }
            result.putBoolean("Result", true);
            result.putString("Message", "");
            return result;

        }

        protected void onPostExecute(Bundle result) {

            if (mCallbacks != null) {
                mCallbacks.onDownloadTrackIdsPostExecute(result);
            }
            mState = State.Stopped;
            mDownloadTrackIds=null;
        }
    }
    */
    // ---------- DownloadTrackIds2 AsyncTask
    /*
    private class DownloadTrackIds2 extends AsyncTask<Bundle, Void, Bundle> {
        boolean mNotify=false;

        protected Bundle doInBackground(Bundle... params) {

            mNotify = params[0].getBoolean("Notify");
            int tracksLimit = params[0].getInt("TracksLimit");

            Bundle result = new Bundle();
            Timber.v("DownloadTrackIds : request tracks on tags : " + mTags);

            String requestUrl = REQUEST_TRACKS_URL_WITH_TAGS;
            requestUrl += mTags;
            requestUrl += "&limit=" + String.valueOf(tracksLimit);
            requestUrl += "&offset=" + String.valueOf(new Random().nextInt(1000));
            requestUrl += "&client_id=" + CLIENT_ID;

            try {
                Request request = new Request.Builder()
                        .url(requestUrl).build();

                Response response = mSoundCloudClient.newCall(request).execute();
                int code = response.code();
                String responseStr = response.body().string();
                if (code == HTTP_OK) {
                    // Parse the response:
                    JSONArray tracksJSON = new JSONArray(responseStr);
                    Timber.v("getTracksInBackground : found " + tracksJSON.length() + " tracks");

                    for (int i = 0; i < tracksJSON.length(); i++) {
                        JSONObject trackJSON = tracksJSON.getJSONObject(i);
                        if (trackJSON.getBoolean("streamable")) {
                            String id = trackJSON.getString("id");
                            mTracks.add(id);
                        }
                    }
                } else {
                    Timber.e("getTracksInBackground : request error : " + responseStr);

                    result.putBoolean("Result", false);
                    result.putString("Message", getString(R.string.app_err));
                    return result;
                }
            } catch (IOException e) {
//                    e.printStackTrace();
                Timber.i(e, "getTracksInBackground : SoundCloud get request error : " + e.getMessage());
                result.putBoolean("Result", false);
                result.putString("Message", getString(R.string.connx_err));
                return result;
            } catch (JSONException e) {
//                    e.printStackTrace();
                Timber.e(e, "getTracksInBackground : JSON parse error : " + e.getMessage());
                result.putBoolean("Result", false);
                result.putString("Message", getString(R.string.app_err));
                return result;
            }
            result.putBoolean("Result", true);
            result.putString("Message", "");
            return result;

        }

        protected void onPostExecute(Bundle result) {

            if (mCallbacks != null && mNotify) {
                mCallbacks.onDownloadTrackIdsPostExecute(result);
            }
            mState = State.Stopped;
            mDownloadTrackIds2=null;
        }
    }
    */



}
