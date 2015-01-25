package com.vfdev.getworkdonemusicapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;

/**
 * About helper class
 */
public class About {

    static void show(Activity activity) {

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        LayoutInflater inflater = activity.getLayoutInflater();

        builder.setTitle(R.string.action_about)
                .setView(inflater.inflate(R.layout.alertdialog_about, null))
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK, so save the mSelectedItems results somewhere
                        // or return them to the component that opened the dialog

                    }
                });
        AlertDialog dialog = builder.create();

        dialog.show();

    }

}
