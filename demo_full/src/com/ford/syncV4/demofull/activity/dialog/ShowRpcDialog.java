package com.ford.syncV4.demofull.activity.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.ford.syncV4.demofull.R;
import com.ford.syncV4.library.persistance.ConnectionPreferences;
import com.ford.syncV4.proxy.rpc.Show;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

public class ShowRpcDialog extends AlertDialog {

    private final int correlationId;
    private final ShowDialogClickListener clickListener;
    private AlertDialog alertDialog;

    public interface ShowDialogClickListener {
        void onSendShow(Show message);
    }

    protected ShowRpcDialog(Context context, int correlationId, ShowDialogClickListener clickListener) {
        super(context);
        this.correlationId = correlationId;
        this.clickListener = clickListener;
        initialize();
    }

    private void initialize() {
        //something
        AlertDialog.Builder builder;

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.dialog_show, null);
        final EditText txtShowField1 = (EditText) layout.findViewById(R.id.txtShowField1);
        final EditText txtShowField2 = (EditText) layout.findViewById(R.id.txtShowField2);
        final EditText statusBar = (EditText) layout.findViewById(R.id.txtStatusBar);
        final EditText mediaClock = (EditText) layout.findViewById(R.id.txtMediaClock);
        final EditText mediaTrack = (EditText) layout.findViewById(R.id.txtMediaTrack);

        if (!getIsMedia()) {
            int visibility = View.GONE;
            mediaClock.setVisibility(visibility);
            mediaTrack.setVisibility(visibility);
            layout.findViewById(R.id.lblMediaTrack).setVisibility(visibility);
            layout.findViewById(R.id.lblMediaClock).setVisibility(visibility);
        }

        builder = new AlertDialog.Builder(getContext());
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Show msg = new Show();
                msg.setCorrelationID(correlationId);
                msg.setMainField1(txtShowField1.getText().toString());
                msg.setMainField2(txtShowField2.getText().toString());
                msg.setStatusBar(statusBar.getText().toString());
                if (getIsMedia()) {
                    msg.setMediaClock(mediaClock.getText().toString());
                    msg.setMediaTrack(mediaTrack.getText().toString());
                }
                clickListener.onSendShow(msg);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.setView(layout);
        alertDialog = builder.create();
    }

    private boolean getIsMedia() {
        ConnectionPreferences connectionPreferences = new ConnectionPreferences(getContext());
        return connectionPreferences.isAnMediaApp();
    }

    @Override
    public void show() {
        alertDialog.show();
    }
}
