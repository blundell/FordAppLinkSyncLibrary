package com.ford.syncV4.android.activity.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;

import com.ford.syncV4.proxy.rpc.SubscribeButton;
import com.ford.syncV4.proxy.rpc.UnsubscribeButton;
import com.ford.syncV4.proxy.rpc.enums.ButtonName;

import java.util.Arrays;
import java.util.List;

public class SubscriptionRpcDialog extends AlertDialog {
    private static boolean[] isButtonSubscribed = null;
    private final int correlationId;
    private final SubscriptionDialogClickListener clickListener;
    private AlertDialog alertDialog;

    private ArrayAdapter<ButtonName> _buttonAdapter = null;

    public static void setSubscribedButtons(List<ButtonName> buttons) {
        List<ButtonName> buttonNames = Arrays.asList(ButtonName.values());
        for (ButtonName buttonName : buttons) {
            isButtonSubscribed[buttonNames.indexOf(buttonName)] = true;
        }
    }

    public static void resetSubscribedButtons() {
        List<ButtonName> subscribableButtonNames = Arrays.asList(ButtonName.values()).subList(0, ButtonName.values().length - 1);
        isButtonSubscribed = new boolean[subscribableButtonNames.size()];
    }

    public interface SubscriptionDialogClickListener {
        void onSendSubscribe(SubscribeButton message);

        void onSendUnSubscribe(UnsubscribeButton message);
    }

    protected SubscriptionRpcDialog(Context context, int correlationId, SubscriptionDialogClickListener clickListener) {
        super(context);
        this.correlationId = correlationId;
        this.clickListener = clickListener;
        resetSubscribedButtons();
        resetAdapter();
        initialize();
    }

    private void resetAdapter() {
        List<ButtonName> subscribableButtonNames = Arrays.asList(ButtonName.values());
        isButtonSubscribed = new boolean[subscribableButtonNames.size()];
        _buttonAdapter = new ArrayAdapter<ButtonName>(getContext(), android.R.layout.select_dialog_multichoice, subscribableButtonNames) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                CheckedTextView ret = (CheckedTextView) super.getView(position, convertView, parent);
                ret.setChecked(isButtonSubscribed[position]);
                return ret;
            }
        };
    }

    private void initialize() {
        //something
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setAdapter(_buttonAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                boolean needToSubscribe = !isButtonSubscribed[which];
                ButtonName buttonName = ButtonName.values()[which];
                if (needToSubscribe) {
                    SubscribeButton msg = new SubscribeButton();
                    msg.setCorrelationID(correlationId);
                    msg.setButtonName(buttonName);
                    clickListener.onSendSubscribe(msg);
                } else {
                    UnsubscribeButton msg = new UnsubscribeButton();
                    msg.setCorrelationID(correlationId);
                    msg.setButtonName(buttonName);
                    clickListener.onSendUnSubscribe(msg);
                }
                isButtonSubscribed[which] = !isButtonSubscribed[which];
            }
        });
        alertDialog = builder.create();
    }

    @Override
    public void show() {
        alertDialog.show();
    }
}
