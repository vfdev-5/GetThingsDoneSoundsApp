package com.vfdev.gettingthingsdonemusicapp.Dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.vfdev.gettingthingsdonemusicapp.R;

/**
 * Helper class to create a Settings dialog
 */
public class SettingsDialog implements DialogInterface.OnClickListener {

    SettingsDialogCallback mCallback;
    AlertDialog mDialog;
    EditText mTags;
    CheckBox mSoundCloudCB;
    CheckBox mHearThisAtCB;
    String mTagsStr;
    String[] mProviders;

    public SettingsDialog(Activity activity) {

        mCallback = (SettingsDialogCallback) activity;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setTitle(R.string.settings_dialog_title)
                .setView(activity.getLayoutInflater().inflate(R.layout.alertdialog_settings, null))
                .setPositiveButton(R.string.settings_update, this)
                .setNeutralButton(R.string.settings_default, this);
        mDialog = builder.create();

    }

    public void setSettings(String tags, String[] providers) {
        mTagsStr = tags;
        mProviders = providers;
    }

    public void show() {
        mDialog.show();
        mTags = (EditText) mDialog.findViewById(R.id.tags);
        mTags.setText(mTagsStr);

        mSoundCloudCB = (CheckBox) mDialog.findViewById(R.id.chk_soundcloud);
        mHearThisAtCB = (CheckBox) mDialog.findViewById(R.id.chk_hearthisat);

        mSoundCloudCB.setChecked(false);
        mHearThisAtCB.setChecked(false);
        for (String e : mProviders) {
            if (e.equalsIgnoreCase("SoundCloud")) {
                mSoundCloudCB.setChecked(true);
                continue;
            }
            if (e.equalsIgnoreCase("HearThisAt")) {
                mHearThisAtCB.setChecked(true);
            }
        }

    }


    @Override
    public void onClick(DialogInterface dialog, int id) {

        if (id == DialogInterface.BUTTON_POSITIVE) {
            String tags = mTags.getText().toString();

            int length = 2;
            String [] providers = new String[2];


            if (mCallback != null) {
                mCallback.onUpdate(tags, providers);
            }
        } else if (id == DialogInterface.BUTTON_NEUTRAL) {
            if (mCallback != null) {
                mCallback.onReset();
            }
        }

    }

    // -------- Callback to Activity
    public interface SettingsDialogCallback {
        public void onUpdate(String tags, String [] providers);
        public void onReset();
    }


}
