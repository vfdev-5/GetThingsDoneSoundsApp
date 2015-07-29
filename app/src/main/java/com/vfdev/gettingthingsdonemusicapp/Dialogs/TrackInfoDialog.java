package com.vfdev.gettingthingsdonemusicapp.Dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.media.Image;
import android.text.Html;
import android.text.Layout;
import android.text.method.LinkMovementMethod;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.vfdev.gettingthingsdonemusicapp.R;
import com.vfdev.mimusicservicelib.core.MusicPlayer;
import com.vfdev.mimusicservicelib.core.TrackInfo;

import timber.log.Timber;

/**
 * Created by vfomin on 7/29/15.
 */
public class TrackInfoDialog implements DialogInterface.OnClickListener {

    AlertDialog mDialog;
    TrackInfo mTrackInfo;

    public TrackInfoDialog(Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(activity.getLayoutInflater().inflate(R.layout.alertdialog_trackinfo, null));
        mDialog = builder.create();
    }

    public void setTrackInfo(TrackInfo trackInfo) {
        mTrackInfo = trackInfo;
    }

    public void show() {
        mDialog.show();
        TextView title = (TextView) mDialog.findViewById(R.id.d_title);

        String titleLink = "<a href=\""+mTrackInfo.fullInfo.get("permalink_url")+"\">"+mTrackInfo.title+"</a>";
        title.setText(Html.fromHtml(titleLink));
        title.setMovementMethod (LinkMovementMethod.getInstance());

        TextView duration = (TextView) mDialog.findViewById(R.id.d_duration);
        duration.setText(MusicPlayer.getDuration(mTrackInfo.duration));

        if (!mTrackInfo.tags.isEmpty()) {
            TextView tags = (TextView) mDialog.findViewById(R.id.d_tags);
            tags.setText(mTrackInfo.tags);
        }


        ImageView artwork = (ImageView) mDialog.findViewById(R.id.d_artwork2);
        String artworkUrl = mTrackInfo.fullInfo.get("artwork_url");
        if (artworkUrl.compareTo("null") != 0)
        {
            ImageLoader.getInstance().displayImage(artworkUrl, artwork);
        }
    }


    @Override
    public void onClick(DialogInterface dialog, int id) {

        if (id == DialogInterface.BUTTON_POSITIVE) {
//            String tags = mTags.getText().toString();
//            if (mCallback != null) {
//                mCallback.onUpdateData(tags);
//            }
        } else if (id == DialogInterface.BUTTON_NEUTRAL) {
//            if (mCallback != null) {
//                mCallback.onResetDefault();
//            }
        }
    }



}
