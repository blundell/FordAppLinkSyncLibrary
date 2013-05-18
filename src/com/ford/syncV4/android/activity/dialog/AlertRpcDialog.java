package com.ford.syncV4.android.activity.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.ford.syncV4.android.R;
import com.ford.syncV4.proxy.TTSChunkFactory;
import com.ford.syncV4.proxy.rpc.Alert;
import com.ford.syncV4.proxy.rpc.TTSChunk;

import java.util.Vector;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

public class AlertRpcDialog extends AlertDialog {
    private final AlertDialogClickListener listener;
    private final int correlationId;
    private AlertDialog dlg;

    public interface AlertDialogClickListener {
        void onSendAlert(Alert message);
    }

    protected AlertRpcDialog(Context context, int correlationId, AlertDialogClickListener listener) {
        super(context);
        this.correlationId = correlationId;
        this.listener = listener;
        initialize();
    }

    private void initialize() {
        AlertDialog.Builder builder;

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.dialog_alert, null);
        final EditText txtSpeak = (EditText) layout.findViewById(R.id.txtSpeak);
        final EditText txtAlertField1 = (EditText) layout.findViewById(R.id.txtAlertField1);
        final EditText txtAlertField2 = (EditText) layout.findViewById(R.id.txtAlertField2);
        final EditText txtDuration = (EditText) layout.findViewById(R.id.txtDuration);
        final CheckBox chkPlayTone = (CheckBox) layout.findViewById(R.id.chkPlayTone);

        builder = new AlertDialog.Builder(getContext());
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String toSpeak = txtSpeak.getText().toString();
                Alert msg = new Alert();
                msg.setCorrelationID(correlationId);
                msg.setAlertText1(txtAlertField1.getText().toString());
                msg.setAlertText2(txtAlertField2.getText().toString());
                msg.setDuration(Integer.parseInt(txtDuration.getText().toString()));
                msg.setPlayTone(chkPlayTone.isChecked());
                if (toSpeak.length() > 0) {
                    Vector<TTSChunk> ttsChunks = TTSChunkFactory.createSimpleTTSChunks(toSpeak);
                    msg.setTtsChunks(ttsChunks);
                }
                listener.onSendAlert(msg);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.setView(layout);
        dlg = builder.create();
    }

    @Override
    public void show() {
        dlg.show();
    }
}
