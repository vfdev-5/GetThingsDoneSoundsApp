package com.vfdev.gettingthingsdonemusicapp.Dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.vfdev.gettingthingsdonemusicapp.R;
import com.vfdev.mimusicservicelib.core.MusicPlayer;
import com.vfdev.mimusicservicelib.core.TrackInfo;

import timber.log.Timber;

/**
 * Created by vfomin on 7/29/15.
 */
public class TrackInfoDialog implements DialogInterface.OnClickListener,
        Button.OnClickListener
{

    AlertDialog mDialog;
    TrackInfo mTrackInfo;
    Drawable playIcon;
    Drawable removeIcon;
    Toast mToast;
    int mFlags;
    boolean safeRemoveChecker=true;

    OnPlayButtonListener mPlayButtonListener;
    OnRemoveButtonListener mRemoveButtonListener;

    public static final int NO_BUTTONS = 1;
    public static final int PLAY_BUTTON = 2;
    public static final int REMOVE_BUTTON = 4;

    public TrackInfoDialog(Activity activity) {
        init(activity, NO_BUTTONS, null);
    }

    public TrackInfoDialog(Activity activity, int flags, Toast toast) {
        init(activity, flags, toast);
    }

    protected void init(Activity activity, int flags, Toast toast) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(activity.getLayoutInflater().inflate(R.layout.alertdialog_trackinfo, null));
        mFlags = flags;
        if (mFlags > NO_BUTTONS) {
            builder.setPositiveButton(".",this);
            builder.setNeutralButton(".", this);
            builder.setNegativeButton(".", this);
        }
        mDialog = builder.create();

        if (toast != null) mToast = toast;

        playIcon = activity.getResources().getDrawable(android.R.drawable.ic_media_play);
        removeIcon = activity.getResources().getDrawable(android.R.drawable.ic_menu_delete);
    }

    public void setOnPlayButtonListener(OnPlayButtonListener listener) {
        mPlayButtonListener = listener;
    }

    public void setOnRemoveButtonListener(OnRemoveButtonListener listener) {
        mRemoveButtonListener = listener;
    }


    public void setTrackInfo(TrackInfo trackInfo) {
        mTrackInfo = trackInfo;
    }

    public void show() {
        mDialog.show();

        // setup button icons:
        if (mFlags > NO_BUTTONS) {
            if ((mFlags & PLAY_BUTTON) == PLAY_BUTTON) {
                setupButtonIcon(DialogInterface.BUTTON_POSITIVE, playIcon, "Play");
            }
            else {
                setButtonEnabled(DialogInterface.BUTTON_POSITIVE, false);
            }
            if ((mFlags & REMOVE_BUTTON) == REMOVE_BUTTON) {
                setupButtonIcon(DialogInterface.BUTTON_NEGATIVE, removeIcon, "Remove");
                Button btn = mDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                btn.setOnClickListener(this);
            }
            else {
                setButtonEnabled(DialogInterface.BUTTON_NEGATIVE, false);
            }
            setButtonEnabled(DialogInterface.BUTTON_NEUTRAL, false);
        }

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
        if (id == DialogInterface.BUTTON_POSITIVE &&
                mPlayButtonListener != null) {
            Timber.v("BUTTON_POSITIVE is clicked");
            mPlayButtonListener.onClick();
        }
    }

    @Override
    public void onClick(View view) {
        if (mRemoveButtonListener !=null) {
            if (safeRemoveChecker) {
                if (mToast != null) {
                    mToast.setText(R.string.tid_double_click);
                    mToast.show();
                }
                view.setBackgroundColor(Color.argb(172, 250, 0, 0));
                safeRemoveChecker = false;
                return;
            }
            Timber.v("Remove is clicked");
            mRemoveButtonListener.onClick();
            mDialog.dismiss();
        }

    }

    // ------ Interfaces
    public interface OnPlayButtonListener {
        public void onClick();
    }

    public interface OnRemoveButtonListener {
        public void onClick();
    }

    // ------ Protected methods

    protected void setupButtonIcon(int buttonIdx, Drawable icon, String text) {
        Button btn = mDialog.getButton(buttonIdx);
        btn.setText(text);
//        btn.setBackground(icon);
//        float r = btn.getHeight() * 1.0f / btn.getWidth();
//        Timber.v("Setup button icon : w=" + btn.getWidth() + ", h=" + btn.getHeight());
//        btn.setScaleX(0.5f);
//        btn.setScaleY(0.5f);
    }

    protected void setButtonEnabled(int buttonIdx, boolean value) {
        Button btn = mDialog.getButton(buttonIdx);
        btn.setText("");
        btn.setEnabled(value);
    }



}
