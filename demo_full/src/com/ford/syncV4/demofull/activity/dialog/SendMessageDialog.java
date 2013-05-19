package com.ford.syncV4.demofull.activity.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import com.ford.syncV4.android.R;
import com.ford.syncV4.demofull.logging.Log;
import com.ford.syncV4.proxy.RPCMessage;
import com.ford.syncV4.proxy.TTSChunkFactory;
import com.ford.syncV4.proxy.constants.Names;
import com.ford.syncV4.proxy.rpc.*;
import com.ford.syncV4.proxy.rpc.enums.*;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

public class SendMessageDialog {

    private static final String ButtonSubscriptions = "ButtonSubscriptions";
    private final Context context;
    private int autoIncrementCorrelationId = 101;
    private int autoIncrementChoiceSetId = 1;
    private int autoIncrementChoiceSetIdCommanId = 1;
    private int itemCommandId = 1;
    private int subMenuCommandId = 1000;

    private static final int CHOICESET_ID_UNSET = -1;

    /**
     * Latest choiceSetId, required to add it to the adapter when a successful
     * CreateInteractionChoiceSetResponse comes.
     */
    private int _latestChoiceSetId = CHOICESET_ID_UNSET;

    private ArrayAdapter<SyncSubMenu> _subMenuAdapter = null;
    private ArrayAdapter<Integer> _commandAdapter = null;
    private ArrayAdapter<Integer> _choiceSetAdapter = null;

    public interface SendMessageDialogListener {

        void onSendMessage(RPCMessage message, int correlationIdUsed);
    }

    private SendMessageDialogListener listener;

    public SendMessageDialog(Context context) {
        this.context = context;
        resetAdapters();
        initialise(context);
    }

    private void initialise(final Context context) {
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.select_dialog_item);
        adapter.add(Names.Alert);
        adapter.add(Names.Speak);
        adapter.add(Names.Show);
        adapter.add(ButtonSubscriptions);
        adapter.add(Names.AddCommand);
        adapter.add(Names.DeleteCommand);
        adapter.add(Names.AddSubMenu);
        adapter.add(Names.DeleteSubMenu);
        adapter.add(Names.SetGlobalProperties);
        adapter.add(Names.ResetGlobalProperties);
        adapter.add(Names.SetMediaClockTimer);
        adapter.add(Names.CreateInteractionChoiceSet);
        adapter.add(Names.DeleteInteractionChoiceSet);
        adapter.add(Names.PerformInteraction);
        adapter.add(Names.EncodedSyncPData);

        new AlertDialog.Builder(context)
                .setTitle("Pick a Function")
                .setAdapter(adapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String dialogChoice = adapter.getItem(which);
                        if (dialogChoice.equals(Names.Alert)) {
                            new AlertRpcDialog(context, autoIncrementCorrelationId, new AlertRpcDialog.AlertDialogClickListener() {
                                @Override
                                public void onSendAlert(Alert message) {
                                    informListener(message);
                                }
                            }).show();
                        } else if (dialogChoice.equals(Names.Speak)) {
                            new SpeakRpcDialog(context, autoIncrementCorrelationId, new SpeakRpcDialog.SpeakDialogClickListener() {
                                @Override
                                public void onSendSpeech(Speak message) {
                                    informListener(message);
                                }
                            }).show();
                        } else if (dialogChoice.equals(Names.Show)) {
                            new ShowRpcDialog(context, autoIncrementCorrelationId, new ShowRpcDialog.ShowDialogClickListener() {
                                @Override
                                public void onSendShow(Show message) {
                                    informListener(message);
                                }
                            }).show();
                        } else if (dialogChoice.equals(ButtonSubscriptions)) {
                            new SubscriptionRpcDialog(context, autoIncrementCorrelationId, new SubscriptionRpcDialog.SubscriptionDialogClickListener() {
                                @Override
                                public void onSendSubscribe(SubscribeButton message) {
                                    informListener(message);
                                }

                                @Override
                                public void onSendUnSubscribe(UnsubscribeButton message) {
                                    informListener(message);
                                }
                            }).show();
                        } else if (dialogChoice.equals(Names.AddCommand)) {
                            //something
                            AlertDialog.Builder builder;
                            AlertDialog addCommandDialog;

                            LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
                            View layout = inflater.inflate(R.layout.dialog_addcommand, null);

                            final EditText er = (EditText) layout.findViewById(R.id.command);
                            final EditText editVrSynonym = (EditText) layout.findViewById(R.id.command2);
                            final Spinner s = (Spinner) layout.findViewById(R.id.availableSubmenus);
                            s.setAdapter(_subMenuAdapter);

                            builder = new AlertDialog.Builder(context);
                            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    AddCommand msg = new AddCommand();
                                    msg.setCorrelationID(autoIncrementCorrelationId);
                                    String itemText = er.getText().toString();
                                    SyncSubMenu sm = (SyncSubMenu) s.getSelectedItem();
                                    MenuParams menuParams = new MenuParams();
                                    menuParams.setMenuName(itemText);
                                    menuParams.setPosition(0);
                                    menuParams.setParentID(sm.getSubMenuId());
                                    msg.setMenuParams(menuParams);

                                    String vrSynonym = editVrSynonym.getText().toString();
                                    if (vrSynonym.length() > 0) {
                                        Vector<String> vrCommands = new Vector<String>();
                                        vrCommands.add(vrSynonym);
                                        msg.setVrCommands(vrCommands);
                                    }

                                    int cmdID = itemCommandId++;
                                    msg.setCmdID(cmdID);

                                    informListener(msg);
                                    _commandAdapter.add(cmdID);
                                }
                            });
                            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                            builder.setView(layout);
                            addCommandDialog = builder.create();
                            addCommandDialog.show();
                        } else if (dialogChoice.equals(Names.DeleteCommand)) {
                            //something
                            AlertDialog.Builder builder = new AlertDialog.Builder(adapter.getContext());
                            builder.setAdapter(_commandAdapter, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    DeleteCommand msg = new DeleteCommand();
                                    msg.setCorrelationID(autoIncrementCorrelationId);
                                    int cmdID = _commandAdapter.getItem(which);
                                    msg.setCmdID(cmdID);
                                    informListener(msg);
                                    _commandAdapter.remove(cmdID);
                                }
                            });
                            AlertDialog dlg = builder.create();
                            dlg.show();
                        } else if (dialogChoice.equals(Names.AddSubMenu)) {
                            //something
                            AlertDialog.Builder builder;
                            AlertDialog addSubMenuDialog;

                            LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
                            View layout = inflater.inflate(R.layout.dialog_add_submenu, null);

                            final EditText subMenu = (EditText) layout.findViewById(R.id.submenu_item);

                            builder = new AlertDialog.Builder(context);
                            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    AddSubMenu msg = new AddSubMenu();
                                    msg.setCorrelationID(autoIncrementCorrelationId);
                                    SyncSubMenu sm = new SyncSubMenu();
                                    sm.setName(subMenu.getText().toString());
                                    sm.setSubMenuId(subMenuCommandId++);
                                    addSubMenuToList(sm);
                                    msg.setMenuID(sm.getSubMenuId());
                                    msg.setMenuName(sm.getName());
                                    msg.setPosition(null);
                                    informListener(msg);
                                }
                            });
                            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                            builder.setView(layout);
                            addSubMenuDialog = builder.create();
                            addSubMenuDialog.show();
                        } else if (dialogChoice.equals(Names.DeleteSubMenu)) {
                            //something
                            AlertDialog.Builder builder = new AlertDialog.Builder(adapter.getContext());
                            builder.setAdapter(_subMenuAdapter, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    SyncSubMenu menu = _subMenuAdapter.getItem(which);
                                    if (menu.getSubMenuId() != 0) {
                                        DeleteSubMenu msg = new DeleteSubMenu();
                                        msg.setCorrelationID(autoIncrementCorrelationId);
                                        msg.setMenuID(menu.getSubMenuId());
                                        informListener(msg);

                                        _subMenuAdapter.remove(menu);
                                    } else {
                                        Toast.makeText(context, "Sorry, can't delete top-level menu", Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                            AlertDialog dlg = builder.create();
                            dlg.show();
                        } else if (dialogChoice.equals(Names.SetGlobalProperties)) {
                            sendSetGlobalProperties();
                        } else if (dialogChoice.equals(Names.ResetGlobalProperties)) {
                            sendResetGlobalProperties();
                        } else if (dialogChoice.equals(Names.SetMediaClockTimer)) {
                            //something
                            AlertDialog.Builder builder;
                            AlertDialog dlg;

                            LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
                            View layout = inflater.inflate(R.layout.dialog_set_media_clock, null);
                            final EditText txtHours = (EditText) layout.findViewById(R.id.txtHours);
                            final EditText txtMinutes = (EditText) layout.findViewById(R.id.txtMinutes);
                            final EditText txtSeconds = (EditText) layout.findViewById(R.id.txtSeconds);
                            final Spinner spnUpdateMode = (Spinner) layout.findViewById(R.id.spnUpdateMode);
                            ArrayAdapter<UpdateMode> spinnerAdapter = new ArrayAdapter<UpdateMode>(adapter.getContext(),
                                    android.R.layout.simple_spinner_item, UpdateMode.values());
                            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            spnUpdateMode.setAdapter(spinnerAdapter);
                            builder = new AlertDialog.Builder(context);
                            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    SetMediaClockTimer msg = new SetMediaClockTimer();
                                    msg.setCorrelationID(autoIncrementCorrelationId);
                                    UpdateMode updateMode = (UpdateMode) spnUpdateMode.getSelectedItem();
                                    msg.setUpdateMode(updateMode);
                                    try {
                                        Integer hours = Integer.parseInt(txtHours.getText().toString());
                                        Integer minutes = Integer.parseInt(txtMinutes.getText().toString());
                                        Integer seconds = Integer.parseInt(txtSeconds.getText().toString());
                                        StartTime startTime = new StartTime();
                                        startTime.setHours(hours);
                                        startTime.setMinutes(minutes);
                                        startTime.setSeconds(seconds);
                                        msg.setStartTime(startTime);
                                    } catch (NumberFormatException e) {
                                        // skip setting start time if parsing failed
                                    }
                                    informListener(msg);
                                }
                            });
                            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                            builder.setView(layout);
                            dlg = builder.create();
                            dlg.show();
                        } else if (dialogChoice.equals(Names.CreateInteractionChoiceSet)) {
                            //something
                            AlertDialog.Builder builder;
                            AlertDialog createCommandSet;

                            LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
                            View layout = inflater.inflate(R.layout.dialog_create_interaction_choices, null);

                            final EditText command1 = (EditText) layout.findViewById(R.id.createcommands_command1);
                            final EditText command2 = (EditText) layout.findViewById(R.id.createcommands_command2);
                            final EditText command3 = (EditText) layout.findViewById(R.id.createcommands_command3);
                            final EditText vr1 = (EditText) layout.findViewById(R.id.createcommands_vr1);
                            final EditText vr2 = (EditText) layout.findViewById(R.id.createcommands_vr2);
                            final EditText vr3 = (EditText) layout.findViewById(R.id.createcommands_vr3);
                            final CheckBox choice1 = (CheckBox) layout.findViewById(R.id.createcommands_choice1);
                            final CheckBox choice2 = (CheckBox) layout.findViewById(R.id.createcommands_choice2);
                            final CheckBox choice3 = (CheckBox) layout.findViewById(R.id.createcommands_choice3);

                            builder = new AlertDialog.Builder(context);
                            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    Vector<Choice> commands = new Vector<Choice>();

                                    if (choice1.isChecked()) {
                                        Choice one = new Choice();
                                        one.setChoiceID(autoIncrementChoiceSetIdCommanId++);
                                        one.setMenuName(command1.getText().toString());
                                        one.setVrCommands(new Vector<String>(Arrays.asList(command1.getText().toString(),
                                                vr1.getText().toString())));
                                        commands.add(one);
                                    }

                                    if (choice2.isChecked()) {
                                        Choice two = new Choice();
                                        two.setChoiceID(autoIncrementChoiceSetIdCommanId++);
                                        two.setMenuName(command2.getText().toString());
                                        two.setVrCommands(new Vector<String>(Arrays.asList(command2.getText().toString(),
                                                vr2.getText().toString())));
                                        commands.add(two);
                                    }

                                    if (choice3.isChecked()) {
                                        Choice three = new Choice();
                                        three.setChoiceID(autoIncrementChoiceSetIdCommanId++);
                                        three.setMenuName(command3.getText().toString());
                                        three.setVrCommands(new Vector<String>(Arrays.asList(command3.getText().toString(),
                                                vr3.getText().toString())));
                                        commands.add(three);
                                    }

                                    if (!commands.isEmpty()) {
                                        CreateInteractionChoiceSet msg = new CreateInteractionChoiceSet();
                                        msg.setCorrelationID(autoIncrementCorrelationId);
                                        int choiceSetID = autoIncrementChoiceSetId++;
                                        msg.setInteractionChoiceSetID(choiceSetID);
                                        msg.setChoiceSet(commands);
                                        if (_latestChoiceSetId != CHOICESET_ID_UNSET) {
                                            Log.w("Latest choiceSetId should be unset, but equals to " + _latestChoiceSetId);
                                        }
                                        _latestChoiceSetId = choiceSetID;
                                        informListener(msg);
                                    } else {
                                        Toast.makeText(context, "No commands to set", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                            builder.setView(layout);
                            createCommandSet = builder.create();
                            createCommandSet.show();
                        } else if (dialogChoice.equals(Names.DeleteInteractionChoiceSet)) {
                            //something
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setAdapter(_choiceSetAdapter, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    DeleteInteractionChoiceSet msg = new DeleteInteractionChoiceSet();
                                    msg.setCorrelationID(autoIncrementCorrelationId);
                                    int commandSetID = _choiceSetAdapter.getItem(which);
                                    msg.setInteractionChoiceSetID(commandSetID);
                                    informListener(msg);

                                    _choiceSetAdapter.remove(commandSetID);
                                }
                            });
                            AlertDialog dlg = builder.create();
                            dlg.show();
                        } else if (dialogChoice.equals(Names.PerformInteraction)) {
                            //something
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setAdapter(_choiceSetAdapter, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    PerformInteraction msg = new PerformInteraction();
                                    msg.setCorrelationID(autoIncrementCorrelationId);
                                    Vector<Integer> interactionChoiceSetIDs = new Vector<Integer>();
                                    interactionChoiceSetIDs.add(_choiceSetAdapter.getItem(which));
                                    Vector<TTSChunk> initChunks = TTSChunkFactory
                                            .createSimpleTTSChunks("Pick a command");
                                    Vector<TTSChunk> helpChunks = TTSChunkFactory
                                            .createSimpleTTSChunks("help me, I'm melting");
                                    Vector<TTSChunk> timeoutChunks = TTSChunkFactory
                                            .createSimpleTTSChunks("hurry it up");
                                    msg.setInitialPrompt(initChunks);
                                    msg.setInitialText("Pick number:");
                                    msg.setInteractionChoiceSetIDList(interactionChoiceSetIDs);
                                    msg.setInteractionMode(InteractionMode.BOTH);
                                    msg.setTimeout(10000);
                                    msg.setHelpPrompt(helpChunks);
                                    msg.setTimeoutPrompt(timeoutChunks);
                                    informListener(msg);
                                }
                            });
                            AlertDialog dlg = builder.create();
                            dlg.show();
                        } else if (dialogChoice.equals(Names.EncodedSyncPData)) {
                            //EncodedSyncPData
                            EncodedSyncPData msg = new EncodedSyncPData();
                            Vector<String> syncPData = new Vector<String>();
                            syncPData.add("AAM4AAkAAAAAAAAAAAA=");
                            msg.setData(syncPData);
                            msg.setCorrelationID(autoIncrementCorrelationId);

                            informListener(msg);
                        }
                    }

                    private void sendSetGlobalProperties() {
                        AlertDialog.Builder builder;

                        LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
                        View layout = inflater.inflate(R.layout.dialog_set_global_properties, null);

                        final EditText helpPrompt = (EditText) layout.findViewById(R.id.setglobalproperties_helpPrompt);
                        final EditText timeoutPrompt = (EditText) layout.findViewById(R.id.setglobalproperties_timeoutPrompt);
                        final CheckBox choiceHelpPrompt = (CheckBox) layout.findViewById(R.id.setglobalproperties_choiceHelpPrompt);
                        final CheckBox choiceTimeoutPrompt = (CheckBox) layout.findViewById(R.id.setglobalproperties_choiceTimeoutPrompt);

                        builder = new AlertDialog.Builder(context);
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                SetGlobalProperties msg = new SetGlobalProperties();
                                int numberOfChoices = 0;

                                if (choiceHelpPrompt.isChecked()) {
                                    Vector<TTSChunk> help = new Vector<TTSChunk>();
                                    help.add(TTSChunkFactory.createChunk(SpeechCapabilities.TEXT, helpPrompt.getText().toString()));
                                    msg.setHelpPrompt(help);
                                    ++numberOfChoices;
                                }

                                if (choiceTimeoutPrompt.isChecked()) {
                                    Vector<TTSChunk> timeout = new Vector<TTSChunk>();
                                    timeout.add(TTSChunkFactory.createChunk(SpeechCapabilities.TEXT, timeoutPrompt.getText().toString()));
                                    msg.setTimeoutPrompt(timeout);
                                    ++numberOfChoices;
                                }

                                if (numberOfChoices > 0) {
                                    msg.setCorrelationID(autoIncrementCorrelationId);
                                    informListener(msg);
                                } else {
                                    Toast.makeText(context, "No items selected", Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                        builder.setView(layout);
                        builder.create().show();
                    }

                    private void sendResetGlobalProperties() {
                        AlertDialog.Builder builder;

                        LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
                        View layout = inflater.inflate(R.layout.dialog_reset_global_properties, null);

                        final CheckBox choiceHelpPrompt = (CheckBox) layout.findViewById(R.id.resetglobalproperties_choiceHelpPrompt);
                        final CheckBox choiceTimeoutPrompt = (CheckBox) layout.findViewById(R.id.resetglobalproperties_choiceTimeoutPrompt);

                        builder = new AlertDialog.Builder(context);
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                ResetGlobalProperties msg = new ResetGlobalProperties();
                                Vector<GlobalProperty> properties = new Vector<GlobalProperty>();

                                if (choiceHelpPrompt.isChecked()) {
                                    properties.add(GlobalProperty.HELPPROMPT);
                                }

                                if (choiceTimeoutPrompt.isChecked()) {
                                    properties.add(GlobalProperty.TIMEOUTPROMPT);
                                }

                                if (!properties.isEmpty()) {
                                    msg.setProperties(properties);
                                    msg.setCorrelationID(autoIncrementCorrelationId);
                                    informListener(msg);
                                } else {
                                    Toast.makeText(context, "No items selected", Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                        builder.setView(layout);
                        builder.create().show();
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void informListener(RPCMessage msg) {
        if (listener != null) {
            listener.onSendMessage(msg, autoIncrementCorrelationId);
        }
        autoIncrementCorrelationId++;
    }

    public void setListener(SendMessageDialogListener listener) {
        this.listener = listener;
    }

    /**
     * Initializes/resets the adapters keeping created submenus, interaction choice set ids, etc.
     */
    public void resetAdapters() {
        SubscriptionRpcDialog.resetSubscribedButtons();

        _subMenuAdapter = new ArrayAdapter<SyncSubMenu>(context, android.R.layout.select_dialog_item);
        _subMenuAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Add top level menu with parent ID zero
        SyncSubMenu sm = new SyncSubMenu();
        sm.setName("Top Level Menu");
        sm.setSubMenuId(0);
        addSubMenuToList(sm);

        _commandAdapter = new ArrayAdapter<Integer>(context, android.R.layout.select_dialog_item);
        _commandAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        _choiceSetAdapter = new ArrayAdapter<Integer>(context, android.R.layout.select_dialog_item);
        _choiceSetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    private void addSubMenuToList(final SyncSubMenu sm) {
        _subMenuAdapter.add(sm);
    }

    /**
     * Set what buttons are shown as subscribed to when you open the dialog
     *
     * @param buttons the list of subscribed buttons
     */
    public void setSubscribedButtons(List<ButtonName> buttons) {
        SubscriptionRpcDialog.setSubscribedButtons(buttons);
    }

    /**
     * Called when a CreateChoiceSetResponse comes. If successful, add it to the adapter. In any case, remove the choice
     */
    public void updateChoiceSet(boolean success) {
        if (_latestChoiceSetId != CHOICESET_ID_UNSET) {
            if (success) {
                _choiceSetAdapter.add(_latestChoiceSetId);
            }
            _latestChoiceSetId = CHOICESET_ID_UNSET;
        } else {
            Log.w("Latest choiceSetId is unset");
        }
    }
}
