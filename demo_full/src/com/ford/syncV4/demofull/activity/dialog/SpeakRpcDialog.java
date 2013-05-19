package com.ford.syncV4.demofull.activity.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.ford.syncV4.demofull.R;
import com.ford.syncV4.proxy.TTSChunkFactory;
import com.ford.syncV4.proxy.rpc.Speak;
import com.ford.syncV4.proxy.rpc.TTSChunk;
import com.ford.syncV4.proxy.rpc.enums.SpeechCapabilities;

import java.util.Vector;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

public class SpeakRpcDialog extends AlertDialog {

    private final int correlationId;
    private final SpeakDialogClickListener clickListener;
    private AlertDialog alertDialog;

    public interface SpeakDialogClickListener {
        void onSendSpeech(Speak message);
    }

    protected SpeakRpcDialog(Context context, int correlationId, SpeakDialogClickListener clickListener) {
        super(context);
        this.correlationId = correlationId;
        this.clickListener = clickListener;
        initialize();
    }

    private void initialize() {
        AlertDialog.Builder builder;

        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.dialog_speak, null);
        final EditText txtSpeakText1 = (EditText) layout.findViewById(R.id.txtSpeakText1);
        final EditText txtSpeakText2 = (EditText) layout.findViewById(R.id.txtSpeakText2);
        final EditText txtSpeakText3 = (EditText) layout.findViewById(R.id.txtSpeakText3);
        final EditText txtSpeakText4 = (EditText) layout.findViewById(R.id.txtSpeakText4);

        final Spinner spnSpeakType1 = (Spinner) layout.findViewById(R.id.spnSpeakType1);
        final Spinner spnSpeakType2 = (Spinner) layout.findViewById(R.id.spnSpeakType2);
        final Spinner spnSpeakType3 = (Spinner) layout.findViewById(R.id.spnSpeakType3);
        final Spinner spnSpeakType4 = (Spinner) layout.findViewById(R.id.spnSpeakType4);

        ArrayAdapter<SpeechCapabilities> speechSpinnerAdapter = new ArrayAdapter<SpeechCapabilities>(getContext(), android.R.layout.simple_spinner_item, SpeechCapabilities.values());
        spnSpeakType1.setAdapter(speechSpinnerAdapter);
        spnSpeakType2.setAdapter(speechSpinnerAdapter);
        spnSpeakType2.setSelection(3);
        spnSpeakType3.setAdapter(speechSpinnerAdapter);
        spnSpeakType4.setAdapter(speechSpinnerAdapter);
        spnSpeakType4.setSelection(1);
        spnSpeakType4.setEnabled(false);

        builder = new AlertDialog.Builder(getContext());
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Speak msg = new Speak();
                msg.setCorrelationID(correlationId);
                String speak1 = txtSpeakText1.getText().toString();
                String speak2 = txtSpeakText2.getText().toString();
                String speak3 = txtSpeakText3.getText().toString();
                String speak4 = txtSpeakText4.getText().toString();
                Vector<TTSChunk> chunks = new Vector<TTSChunk>();

                if (speak1.length() > 0) {
                    chunks.add(TTSChunkFactory.createChunk((SpeechCapabilities) spnSpeakType1.getSelectedItem(), speak1));

                }
                if (speak2.length() > 0) {
                    chunks.add(TTSChunkFactory.createChunk((SpeechCapabilities) spnSpeakType2.getSelectedItem(), speak2));

                }
                if (speak3.length() > 0) {
                    chunks.add(TTSChunkFactory.createChunk((SpeechCapabilities) spnSpeakType3.getSelectedItem(), speak3));

                }
                if (speak4.length() > 0) {
                    chunks.add(TTSChunkFactory.createChunk(SpeechCapabilities.SAPI_PHONEMES, speak4));

                }
                msg.setTtsChunks(chunks);
                clickListener.onSendSpeech(msg);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.setView(layout);
        alertDialog = builder.create();
    }

    @Override
    public void show() {
        alertDialog.show();
    }
}
