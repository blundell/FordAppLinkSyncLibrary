package com.ford.syncV4.android.activity.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.ford.syncV4.android.R;
import com.ford.syncV4.android.persistance.ConnectionPreferences;
import com.ford.syncV4.android.persistance.Const;
import com.ford.syncV4.proxy.rpc.enums.Language;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

public class PropertiesDialog extends AlertDialog {

    private PropertiesDialogClickListener clickListener;

    public interface PropertiesDialogClickListener {
        void onPropertiesSelected();
    }

    public PropertiesDialog(Context context) {
        super(context, android.R.style.Theme_DeviceDefault_Dialog);
//        initialise(context);
    }

    private void initialise(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_properties, (ViewGroup) findViewById(R.id.properties_Root));

        ArrayAdapter<Language> langAdapter = new ArrayAdapter<Language>(context, android.R.layout.simple_spinner_item, Language.values());
        langAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        final CheckBox mediaCheckBox = (CheckBox) view.findViewById(R.id.properties_checkMedia);
        final RadioGroup transportGroup = (RadioGroup) view.findViewById(R.id.properties_radioGroupTransport);
        final EditText ipAddressEditText = (EditText) view.findViewById(R.id.properties_ipAddr);
        final EditText tcpPortEditText = (EditText) view.findViewById(R.id.properties_tcpPort);
        final CheckBox autoReconnectCheckBox = (CheckBox) view.findViewById(R.id.properties_checkAutoReconnect);

        ipAddressEditText.setEnabled(false);
        tcpPortEditText.setEnabled(false);
        autoReconnectCheckBox.setEnabled(false);

        transportGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                boolean transportOptionsEnabled = checkedId == R.id.properties_radioWiFi;
                ipAddressEditText.setEnabled(transportOptionsEnabled);
                tcpPortEditText.setEnabled(transportOptionsEnabled);
                autoReconnectCheckBox.setEnabled(transportOptionsEnabled);
            }
        });

        final ConnectionPreferences connectionPreferences = new ConnectionPreferences(getContext());
        boolean isMedia = connectionPreferences.isAnMediaApp();
        int transportType = connectionPreferences.getTransportType();
        String ipAddress = connectionPreferences.getIpAddress();
        int tcpPort = connectionPreferences.getTcpPort();
        boolean autoReconnect = connectionPreferences.shouldAutoReconnect();

        mediaCheckBox.setChecked(isMedia);
        transportGroup.check(transportType == Const.Transport.KEY_TCP ? R.id.properties_radioWiFi : R.id.properties_radioBT);
        ipAddressEditText.setText(ipAddress);
        tcpPortEditText.setText(String.valueOf(tcpPort));
        autoReconnectCheckBox.setChecked(autoReconnect);

        setTitle("Please select properties");
        setCancelable(false);
        setButton("OK", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                boolean isMedia = mediaCheckBox.isChecked();
                int transportType = transportGroup.getCheckedRadioButtonId() == R.id.properties_radioWiFi ? Const.Transport.KEY_TCP
                        : Const.Transport.KEY_BLUETOOTH;
                String ipAddress = ipAddressEditText.getText().toString();
                int tcpPort = Integer.parseInt(tcpPortEditText.getText().toString());
                boolean autoReconnect = autoReconnectCheckBox.isChecked();

                // save the configs
                connectionPreferences.saveIsAnMediaApp(isMedia);
                connectionPreferences.saveTransportType(transportType);
                connectionPreferences.saveTransportAddress(ipAddress, tcpPort);
                connectionPreferences.saveShouldAutoReconnect(autoReconnect);

                if (clickListener != null) {
                    clickListener.onPropertiesSelected();
                }
            }
        });
        setView(view);
    }

    public Builder temp() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_properties, (ViewGroup) findViewById(R.id.properties_Root));

        ArrayAdapter<Language> langAdapter = new ArrayAdapter<Language>(getContext(), android.R.layout.simple_spinner_item, Language.values());
        langAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        final CheckBox mediaCheckBox = (CheckBox) view.findViewById(R.id.properties_checkMedia);
        final RadioGroup transportGroup = (RadioGroup) view.findViewById(R.id.properties_radioGroupTransport);
        final EditText ipAddressEditText = (EditText) view.findViewById(R.id.properties_ipAddr);
        final EditText tcpPortEditText = (EditText) view.findViewById(R.id.properties_tcpPort);
        final CheckBox autoReconnectCheckBox = (CheckBox) view.findViewById(R.id.properties_checkAutoReconnect);

        ipAddressEditText.setEnabled(false);
        tcpPortEditText.setEnabled(false);
        autoReconnectCheckBox.setEnabled(false);

        transportGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                boolean transportOptionsEnabled = checkedId == R.id.properties_radioWiFi;
                ipAddressEditText.setEnabled(transportOptionsEnabled);
                tcpPortEditText.setEnabled(transportOptionsEnabled);
                autoReconnectCheckBox.setEnabled(transportOptionsEnabled);
            }
        });

        // display current configs
        final ConnectionPreferences connectionPreferences = new ConnectionPreferences(getContext());
        boolean isMedia = connectionPreferences.isAnMediaApp();
        int transportType = connectionPreferences.getTransportType();
        String ipAddress = connectionPreferences.getIpAddress();
        int tcpPort = connectionPreferences.getTcpPort();
        boolean autoReconnect = connectionPreferences.shouldAutoReconnect();

        mediaCheckBox.setChecked(isMedia);
        transportGroup.check(transportType == Const.Transport.KEY_TCP ? R.id.properties_radioWiFi : R.id.properties_radioBT);
        ipAddressEditText.setText(ipAddress);
        tcpPortEditText.setText(String.valueOf(tcpPort));
        autoReconnectCheckBox.setChecked(autoReconnect);

        return new Builder(getContext()).
                setTitle("Please select properties").
                setCancelable(false).
                setPositiveButton(android.R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        boolean isMedia = mediaCheckBox.isChecked();
                        int transportType = transportGroup
                                .getCheckedRadioButtonId() == R.id.properties_radioWiFi ? Const.Transport.KEY_TCP
                                : Const.Transport.KEY_BLUETOOTH;
                        String ipAddress = ipAddressEditText.getText().toString();
                        int tcpPort = Integer.parseInt(tcpPortEditText.getText().toString());
                        boolean autoReconnect = autoReconnectCheckBox.isChecked();

                        // save the configs
                        connectionPreferences.saveIsAnMediaApp(isMedia);
                        connectionPreferences.saveTransportType(transportType);
                        connectionPreferences.saveTransportAddress(ipAddress, tcpPort);
                        connectionPreferences.saveShouldAutoReconnect(autoReconnect);

                        if (clickListener != null) {
                            clickListener.onPropertiesSelected();
                        }
                    }
                }).
                setView(view);
    }

    public PropertiesDialog setOnClickListener(PropertiesDialogClickListener clickListener) {
        this.clickListener = clickListener;
        return this;
    }

}
