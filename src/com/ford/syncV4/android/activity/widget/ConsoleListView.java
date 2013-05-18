package com.ford.syncV4.android.activity.widget;

import android.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.ford.syncV4.android.logging.Log;
import com.ford.syncV4.proxy.RPCMessage;

import org.json.JSONException;

public class ConsoleListView extends ListView {

    public ConsoleListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialise();
    }

    public ConsoleListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialise();
    }

    private void initialise() {
        setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);

        setOnItemClickListener(onItemClickListener);
    }

    private final OnItemClickListener onItemClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Object listObj = parent.getItemAtPosition(position);
            if (listObj instanceof RPCMessage) {
                String rawJSON = "";
                try {
                    rawJSON = ((RPCMessage) listObj).serializeJSON().toString(2);
                } catch (JSONException e) {
                    Log.e("ConsoleListView", e);
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Raw JSON");
                builder.setMessage(rawJSON);
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                AlertDialog ad = builder.create();
                ad.show();
            } else if (listObj instanceof String) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage(listObj.toString());
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                AlertDialog ad = builder.create();
                ad.show();
            }
        }
    };
}
