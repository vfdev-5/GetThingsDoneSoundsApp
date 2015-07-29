package com.vfdev.gettingthingsdonemusicapp.Dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
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
    String mData;

    public SettingsDialog(Activity activity) {

        mCallback = (SettingsDialogCallback) activity;

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setTitle(R.string.settings_dialog_title)
                .setView(activity.getLayoutInflater().inflate(R.layout.alertdialog_settings, null))
                .setPositiveButton(R.string.settings_update, this)
                .setNeutralButton(R.string.settings_default, this);
        mDialog = builder.create();

    }

    public void setData(String data) {
        mData = data;
    }

    public void show() {
        mDialog.show();
        mTags = (EditText) mDialog.findViewById(R.id.tags);
        mTags.setText(mData);
    }


    @Override
    public void onClick(DialogInterface dialog, int id) {

        if (id == DialogInterface.BUTTON_POSITIVE) {
            String tags = mTags.getText().toString();
            if (mCallback != null) {
                mCallback.onUpdateData(tags);
            }
        } else if (id == DialogInterface.BUTTON_NEUTRAL) {
            if (mCallback != null) {
                mCallback.onResetDefault();
            }
        }

    }

    // -------- Callback to Activity
    public interface SettingsDialogCallback {
        public void onUpdateData(String data);
        public void onResetDefault();
    }


}
