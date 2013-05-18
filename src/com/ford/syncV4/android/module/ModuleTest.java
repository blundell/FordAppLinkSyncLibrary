package com.ford.syncV4.android.module;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Pair;
import android.util.Xml;

import com.ford.syncV4.android.adapters.LogAdapter;
import com.ford.syncV4.android.constants.AcceptedRPC;
import com.ford.syncV4.android.logging.Log;
import com.ford.syncV4.android.service.MyAppLinkProxy;
import com.ford.syncV4.exception.SyncException;
import com.ford.syncV4.proxy.RPCRequest;
import com.ford.syncV4.proxy.constants.Names;
import com.ford.syncV4.proxy.rpc.*;
import com.ford.syncV4.proxy.rpc.enums.Result;
import com.ford.syncV4.proxy.rpc.enums.UpdateMode;

import java.io.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

import org.xmlpull.v1.XmlPullParser;

public class ModuleTest {
    private static ModuleTest _instance;
    private static Runnable threadContext;
    private static ModuleTest DialogThreadContext;
    private final MyAppLinkProxy myAppLinkProxy;
    private Activity activity;
    private LogAdapter messageAdapter;
    private Thread workerThread;

    private boolean pass;
    private boolean integration;
    private String userPrompt;

    private int numIterations;

    private ArrayList<Pair<Integer, Result>> expecting = new ArrayList<Pair<Integer, Result>>();
    private ArrayList<Pair<String, ArrayList<RPCRequest>>> testList = new ArrayList<Pair<String, ArrayList<RPCRequest>>>();

    public static ArrayList<Pair<Integer, Result>> responses = new ArrayList<Pair<Integer, Result>>();

    public ModuleTest(MyAppLinkProxy myAppLinkProxy, LogAdapter messageAdapter, Activity activity) {
        this.myAppLinkProxy = myAppLinkProxy;
        this.messageAdapter = messageAdapter;
        this.activity = activity;

        _instance = this;

        workerThread = makeThread();
    }

    public void runTests() {
        workerThread.start();
    }

    public void restart() {
        workerThread.interrupt();
        workerThread = null;
        workerThread = makeThread();
        runTests();
    }

    private String[] mFileList;
    //private File mPath = new File(Environment.getExternalStorageDirectory() + "");//"//yourdir//");
    //private File mPath = new File("/sdcard/");
    private File mPath = new File(Environment.getExternalStorageDirectory() + "");
    private String mChosenFile;
    private static final String FTYPE = ".xml";
    private static final int DIALOG_LOAD_FILE = 1000;

    private void loadFileList() {
        try {
            mPath.mkdirs();
        } catch (SecurityException e) {
            Log.e("unable to write on the sd card " + e.toString());
        }
        if (mPath.exists()) {
            FilenameFilter filter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    File sel = new File(dir, filename);
                    return filename.contains(FTYPE);// || sel.isDirectory();
                }
            };
            mFileList = mPath.list(filter);
        } else {
            mFileList = new String[0];
        }
    }

    Dialog dialog;

    private Dialog onCreateDialog(final int id) {
        DialogThreadContext = this;
        //Dialog dialog = null;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog = null;
                AlertDialog.Builder builder = new Builder(activity);

                switch (id) {
                    case DIALOG_LOAD_FILE:
                        builder.setTitle("Choose your file");
                        if (mFileList == null) {
                            Log.e("Showing file picker before loading the file list");
                            dialog = builder.create();
                            //return dialog;
                            synchronized (DialogThreadContext) {
                                DialogThreadContext.notify();
                            }
                            Thread.currentThread().interrupt();
                        }
                        builder.setItems(mFileList, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mChosenFile = mFileList[which];
                                //you can do stuff with the file here too
                                synchronized (DialogThreadContext) {
                                    DialogThreadContext.notify();
                                }
                                Thread.currentThread().interrupt();
                            }
                        });
                        break;
                    default:
                        break;
                }
                dialog = builder.show();
            }
        });

        try {
            synchronized (this) {
                this.wait();
            }
        } catch (InterruptedException e) {
            Log.e("InterruptedException", e);
        }
        return dialog;
    }

    private Thread makeThread() {
        return new Thread(new Runnable() {
            @Override
            public void run() {
                mChosenFile = null;
                loadFileList();
                onCreateDialog(DIALOG_LOAD_FILE);
                if (mChosenFile != null) {
                    AcceptedRPC acceptedRPC = new AcceptedRPC();
                    XmlPullParser parser = Xml.newPullParser();
                    RPCRequest rpc;
                    try {
                        //FileInputStream fin = new FileInputStream("/sdcard/test.xml");
                        FileInputStream fin = new FileInputStream("/sdcard/" + mChosenFile);
                        InputStreamReader isr = new InputStreamReader(fin);

                        String outFile = "/sdcard/" + mChosenFile.substring(0, mChosenFile.length() - 4) + ".csv";
                        File out = new File(outFile);
                        FileWriter writer = new FileWriter(out);
                        writer.flush();

                        parser.setInput(isr);
                        int eventType = parser.getEventType();
                        String name;
                        boolean done = false;
                        while (eventType != XmlPullParser.END_DOCUMENT && !done) {
                            name = parser.getName();

                            switch (eventType) {
                                case XmlPullParser.START_DOCUMENT:
                                    Log.e("START_DOCUMENT, name: " + name);
                                    break;
                                case XmlPullParser.END_DOCUMENT:
                                    Log.e("END_DOCUMENT, name: " + name);
                                    break;
                                case XmlPullParser.START_TAG:
                                    name = parser.getName();
                                    if (name.equalsIgnoreCase("test")) {
                                        messageAdapter.logMessage("test " + parser.getAttributeValue(0), true);
                                        testList.add(new Pair<String, ArrayList<RPCRequest>>(parser.getAttributeValue(0), new ArrayList<RPCRequest>()));
                                        expecting.clear();
                                        responses.clear();
                                        try {
                                            if (parser.getAttributeName(1) != null) {
                                                if (parser.getAttributeName(1).equalsIgnoreCase("iterations")) {
                                                    try {
                                                        numIterations = Integer.parseInt(parser.getAttributeValue(1));
                                                    } catch (Exception e) {
                                                        Log.e("Unable to parse number of iterations", e);
                                                    }
                                                } else numIterations = 1;
                                            } else numIterations = 1;
                                        } catch (Exception e) {
                                            numIterations = 1;
                                        }
                                    } else if (name.equalsIgnoreCase("type")) {
                                        if (parser.getAttributeValue(0).equalsIgnoreCase("integration")) integration = true;
                                        else if (parser.getAttributeValue(0).equalsIgnoreCase("unit")) integration = false;
                                    } else if (acceptedRPC.isAcceptedRPC(name)) {
                                        //Create correct object
                                        if (name.equalsIgnoreCase(Names.RegisterAppInterface)) {
                                            rpc = new RegisterAppInterface();
                                        } else if (name.equalsIgnoreCase(Names.UnregisterAppInterface)) {
                                            rpc = new UnregisterAppInterface();
                                        } else if (name.equalsIgnoreCase(Names.SetGlobalProperties)) {
                                            rpc = new SetGlobalProperties();
                                        } else if (name.equalsIgnoreCase(Names.ResetGlobalProperties)) {
                                            rpc = new ResetGlobalProperties();
                                        } else if (name.equalsIgnoreCase(Names.AddCommand)) {
                                            rpc = new AddCommand();
                                        } else if (name.equalsIgnoreCase(Names.DeleteCommand)) {
                                            rpc = new DeleteCommand();
                                        } else if (name.equalsIgnoreCase(Names.AddSubMenu)) {
                                            rpc = new AddSubMenu();
                                        } else if (name.equalsIgnoreCase(Names.DeleteSubMenu)) {
                                            rpc = new DeleteSubMenu();
                                        } else if (name.equalsIgnoreCase(Names.CreateInteractionChoiceSet)) {
                                            rpc = new CreateInteractionChoiceSet();
                                        } else if (name.equalsIgnoreCase(Names.PerformInteraction)) {
                                            rpc = new PerformInteraction();
                                        } else if (name.equalsIgnoreCase(Names.DeleteInteractionChoiceSet)) {
                                            rpc = new DeleteInteractionChoiceSet();
                                        } else if (name.equalsIgnoreCase(Names.Alert)) {
                                            rpc = new Alert();
                                        } else if (name.equalsIgnoreCase(Names.Show)) {
                                            rpc = new Show();
                                        } else if (name.equalsIgnoreCase(Names.Speak)) {
                                            rpc = new Speak();
                                        } else if (name.equalsIgnoreCase(Names.SetMediaClockTimer)) {
                                            rpc = new SetMediaClockTimer();
                                        } else if (name.equalsIgnoreCase(Names.EncodedSyncPData)) {
                                            rpc = new EncodedSyncPData();
                                        } else if (name.equalsIgnoreCase(Names.SubscribeButton)) {
                                            rpc = new SubscribeButton();
                                        } else if (name.equalsIgnoreCase(Names.UnsubscribeButton)) {
                                            rpc = new UnsubscribeButton();
                                        } else if (name.equalsIgnoreCase("ClearMediaClockTimer")) {
                                            rpc = new Show();
                                            ((Show) rpc).setMainField1(null);
                                            ((Show) rpc).setMainField2(null);
                                            ((Show) rpc).setStatusBar(null);
                                            ((Show) rpc).setMediaClock("     ");
                                            ((Show) rpc).setMediaTrack(null);
                                            ((Show) rpc).setAlignment(null);
                                        } else if (name.equalsIgnoreCase("PauseMediaClockTimer")) {
                                            rpc = new SetMediaClockTimer();
                                            StartTime startTime = new StartTime();
                                            startTime.setHours(0);
                                            startTime.setMinutes(0);
                                            startTime.setSeconds(0);
                                            ((SetMediaClockTimer) rpc).setStartTime(startTime);
                                            ((SetMediaClockTimer) rpc).setUpdateMode(UpdateMode.PAUSE);
                                        } else if (name.equalsIgnoreCase("ResumeMediaClockTimer")) {
                                            rpc = new SetMediaClockTimer();
                                            StartTime startTime = new StartTime();
                                            startTime.setHours(0);
                                            startTime.setMinutes(0);
                                            startTime.setSeconds(0);
                                            ((SetMediaClockTimer) rpc).setStartTime(startTime);
                                            ((SetMediaClockTimer) rpc).setUpdateMode(UpdateMode.RESUME);
                                        } else {
                                            rpc = new SetGlobalProperties();
                                        }

                                        if (parser.getAttributeName(0) != null &&
                                                parser.getAttributeName(0).equalsIgnoreCase("correlationID")) {
                                            try {
                                                rpc.setCorrelationID(Integer.parseInt(parser.getAttributeValue(0)));
                                            } catch (Exception e) {
                                                Log.e("Unable to parse Integer");
                                            }
                                        }

                                        Hashtable hash = setParams(name, parser);
                                        Log.e("" + hash);
                                        for (Object key : hash.keySet()) {
                                            rpc.setParameters((String) key, hash.get(key));
                                        }

                                        for (Object o : hash.entrySet()) {
                                            Hashtable.Entry pairs = (Hashtable.Entry) o;
                                            System.out.println(pairs.getKey() + " = " + pairs.getValue());
                                        }

                                        Pair<String, ArrayList<RPCRequest>> temp = testList.get(testList.size() - 1);
                                        temp.second.add(rpc);
                                        testList.set(testList.size() - 1, temp);
                                    } else if (name.equalsIgnoreCase("result")) {
                                        expecting.add(new Pair<Integer, Result>(Integer.parseInt(parser.getAttributeValue(0)), (Result.valueForString(parser.getAttributeValue(1)))));
                                    } else if (name.equalsIgnoreCase("userPrompt") && integration) {
                                        userPrompt = parser.getAttributeValue(0);
                                    }
                                    break;
                                case XmlPullParser.END_TAG:
                                    name = parser.getName();
                                    if (name.equalsIgnoreCase("test")) {
                                        try {
                                            boolean localPass = true;
                                            int i = numIterations;
                                            int numPass = 0;
                                            while (i > 0) {
                                                xmlTest();
                                                if (pass) numPass++;
                                                else localPass = false;
                                                i--;
                                            }
                                            if (localPass)
                                                writer.write("" + testList.get(testList.size() - 1).first + ", Pass, " + numPass + ", " + numIterations + "\n");
                                            if (!localPass)
                                                writer.write("" + testList.get(testList.size() - 1).first + ", Fail, " + numPass + ", " + numIterations + "\n");
                                            Log.i(testList.get(testList.size() - 1).first + ", " + localPass + ", " + numPass + ", " + numIterations);
                                            messageAdapter.logMessage("" + testList.get(testList.size() - 1).first + ", " + localPass + ", " + numPass + ", " + numIterations, true);
                                        } catch (Exception e) {
                                            Log.e("Test " + testList.get(testList.size() - 1).first + " Failed! ", e);
                                        }
                                    }
                                    break;
                                case XmlPullParser.TEXT:
                                    //Log.e("TEXT, name: " + name);
                                    break;
                                case XmlPullParser.CDSECT:
                                    Log.e("CDSECT, name: " + name);
                                    break;
                                case XmlPullParser.ENTITY_REF:
                                    Log.e("ENTITY_REF, name: " + name);
                                    break;
                                case XmlPullParser.IGNORABLE_WHITESPACE:
                                    Log.e("IGNORABLE_WHITESPACE, name: " + name);
                                    break;
                                case XmlPullParser.PROCESSING_INSTRUCTION:
                                    Log.e("PROCESSING_INSTRUCTION, name: " + name);
                                    break;
                                case XmlPullParser.COMMENT:
                                    Log.e("COMMENT, name: " + name);
                                    break;
                                case XmlPullParser.DOCDECL:
                                    Log.e("DOCDECL, name: " + name);
                                    break;
                                default:
                                    break;
                            }
                            eventType = parser.next();
                        }
                        writer.close();

                        Intent email = new Intent(Intent.ACTION_SEND);
                        email.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        email.setType("plain/text");
                        email.putExtra(Intent.EXTRA_EMAIL, new String[]{"youremail@ford.com"});
                        email.putExtra(Intent.EXTRA_SUBJECT, "Lua Unit Test Export");
                        email.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(out));

                        activity.startActivity(Intent.createChooser(email, "Choose an Email client :"));

                    } catch (Exception e) {
                        Log.e("Parser Failed!!", e);
                    }
                }
            }
        });
    }

    private Hashtable setParams(String name, XmlPullParser parser) {

        Log.e("setParams start name: " + name);

        Hashtable<String, Object> hash = new Hashtable<String, Object>();

        int eventType = 0;
        Boolean done = false;
        String tempName;
        String vectorName = null;

        try {
            while (eventType != XmlPullParser.END_DOCUMENT && !done) {
                tempName = parser.getName();

                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        Log.e("START_DOCUMENT, tempName: " + tempName);
                        break;
                    case XmlPullParser.END_DOCUMENT:
                        Log.e("END_DOCUMENT, tempName: " + tempName);
                        break;
                    case XmlPullParser.START_TAG:
                        if (tempName.equalsIgnoreCase("Vector")) {
                            Log.e("In Vector");
                            Vector<Object> vector = new Vector<Object>();

                            if (parser.getAttributeName(0) != null) vectorName = parser.getAttributeValue(0);

                            Boolean nestedWhileDone = false;

                            eventType = parser.next();
                            while (eventType != XmlPullParser.START_TAG && !nestedWhileDone) {
                                if (eventType == XmlPullParser.END_TAG) {
                                    if (parser.getName().equalsIgnoreCase("Vector")) {
                                        Log.e("In Vector Loop, nestedWhileDone == true, END_TAG, name: " + name);
                                        nestedWhileDone = true;
                                    }
                                } else eventType = parser.next();
                            }

                            while (eventType != XmlPullParser.END_DOCUMENT && !nestedWhileDone) {
                                tempName = parser.getName();
                                Log.e("In Vector Loop, tempName: " + tempName);

                                switch (eventType) {
                                    case XmlPullParser.START_DOCUMENT:
                                        Log.e("In Vector Loop, START_DOCUMENT, name: " + name);
                                        break;
                                    case XmlPullParser.END_DOCUMENT:
                                        Log.e("In Vector Loop, END_DOCUMENT, name: " + name);
                                        break;
                                    case XmlPullParser.START_TAG:
                                        if (tempName.equalsIgnoreCase("Integer")) {
                                            Log.e("In Nested Vector Integer");
                                            if (parser.getAttributeName(0) != null) {
                                                try {
                                                    vector.add(Double.parseDouble(parser.getAttributeValue(0)));
                                                } catch (Exception e) {
                                                    Log.e("Unable to parse Integer");
                                                }
                                            }
                                        } else if (tempName.equalsIgnoreCase("String")) {
                                            Log.e("In Nested Vector String");
                                            if (parser.getAttributeName(0) != null) {
                                                vector.add(parser.getAttributeValue(0));
                                            }
                                        } else {
                                            vector.add(setParams(tempName, parser));
                                        }
                                        break;
                                    case XmlPullParser.END_TAG:
                                        Log.e("In Vector Loop, END_TAG, name: " + name);
                                        if (tempName.equalsIgnoreCase("Vector")) {
                                            Log.e("In Vector Loop, nestedWhileDone == true, END_TAG, name: " + name);
                                            nestedWhileDone = true;
                                        }
                                        break;
                                    case XmlPullParser.TEXT:
                                        //Log.e("TEXT, name: " + name);
                                        break;
                                    case XmlPullParser.CDSECT:
                                        Log.e("In Vector Loop, CDSECT, name: " + name);
                                        break;
                                    case XmlPullParser.ENTITY_REF:
                                        Log.e("In Vector Loop, ENTITY_REF, name: " + name);
                                        break;
                                    case XmlPullParser.IGNORABLE_WHITESPACE:
                                        Log.e("In Vector Loop, IGNORABLE_WHITESPACE, name: " + name);
                                        break;
                                    case XmlPullParser.PROCESSING_INSTRUCTION:
                                        Log.e("In Vector Loop, PROCESSING_INSTRUCTION, name: " + name);
                                        break;
                                    case XmlPullParser.COMMENT:
                                        Log.e("In Vector Loop, COMMENT, name: " + name);
                                        break;
                                    case XmlPullParser.DOCDECL:
                                        Log.e("In Vector Loop, DOCDECL, name: " + name);
                                        break;
                                    default:
                                        break;
                                }
                                eventType = parser.next();
                            }
                            Log.e("out of Vector loop");
                            hash.put(vectorName, vector);
                        } else if (tempName.equalsIgnoreCase("Integer")) {
                            Log.e("In Integer");
                            if (parser.getAttributeName(0) != null) {
                                try {
                                    hash.put(parser.getAttributeName(0), Double.parseDouble(parser.getAttributeValue(0)));
                                } catch (Exception e) {
                                    Log.e("Unable to parse Integer");
                                }
                            }
                        } else if (tempName.equalsIgnoreCase("Boolean")) {
                            Log.e("In Boolean");
                            if (parser.getAttributeName(0) != null) {
                                if (parser.getAttributeValue(0).equalsIgnoreCase("true")) hash.put(parser.getAttributeName(0), true);
                                else if (parser.getAttributeValue(0).equalsIgnoreCase("false")) hash.put(parser.getAttributeName(0), false);
                            }
                        } else if (tempName.equalsIgnoreCase("String")) {
                            Log.e("In String");
                            if (parser.getAttributeName(0) != null) {
                                hash.put(parser.getAttributeName(0), parser.getAttributeValue(0));
                            }
                        } else {
                            Log.e("Returning in else statement");
                            hash.put(tempName, setParams(tempName, parser));
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if (tempName.equalsIgnoreCase(name)) {
                            done = true;
                        }
                        break;
                    case XmlPullParser.TEXT:
                        //Log.e("TEXT, tempName: " + tempName);
                        break;
                    case XmlPullParser.CDSECT:
                        Log.e("CDSECT, tempName: " + tempName);
                        break;
                    case XmlPullParser.ENTITY_REF:
                        Log.e("ENTITY_REF, tempName: " + tempName);
                        break;
                    case XmlPullParser.IGNORABLE_WHITESPACE:
                        Log.e("IGNORABLE_WHITESPACE, tempName: " + tempName);
                        break;
                    case XmlPullParser.PROCESSING_INSTRUCTION:
                        Log.e("PROCESSING_INSTRUCTION, tempName: " + tempName);
                        break;
                    case XmlPullParser.COMMENT:
                        Log.e("COMMENT, tempName: " + tempName);
                        break;
                    case XmlPullParser.DOCDECL:
                        Log.e("DOCDECL, tempName: " + tempName);
                        break;
                    default:
                        break;
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            Log.e("Parser Failed!!", e);
        }

        Log.e("Returning at end of setParams function");
        return hash;
    }

    private boolean xmlTest() {
        pass = false;

        Thread newThread = new Thread(new Runnable() {
            @Override
            public void run() {
                threadContext = this;

                for (RPCRequest rpc : testList.get(testList.size() - 1).second) {
                    messageAdapter.logMessage(rpc, true);
                    try {
                        myAppLinkProxy.getSyncProxyInstance().sendRPCRequest(rpc);
                    } catch (SyncException e) {
                        Log.e("Error sending RPC", e);
                    }
                }

                try {
                    for (Pair<Integer, Result> anExpecting : expecting) {
                        synchronized (this) {
                            this.wait(10000);
                        }
                    }
                } catch (InterruptedException e) {
                    Log.e("InterruptedException", e);
                }

                try {
                    synchronized (this) {
                        this.wait(5000);
                    }
                } catch (InterruptedException e) {
                    Log.e("InterruptedException", e);
                }

                if (expecting.equals(responses)) {
                    pass = true;
                    if (integration) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                                alert.setMessage(userPrompt);
                                alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        pass = true;
                                        synchronized (threadContext) {
                                            threadContext.notify();
                                        }
                                    }
                                });
                                alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        pass = false;
                                        synchronized (threadContext) {
                                            threadContext.notify();
                                        }
                                    }
                                });
                                alert.show();
                            }
                        });

                        try {
                            synchronized (this) {
                                this.wait();
                            }
                        } catch (InterruptedException e) {
                            Log.e("InterruptedException", e);
                        }
                    }
                }

                synchronized (_instance) {
                    _instance.notify();
                }

                Thread.currentThread().interrupt();
            }
        });
        newThread.start();

        try {
            synchronized (this) {
                this.wait();
            }
        } catch (InterruptedException e) {
            Log.e("InterruptedException", e);
        }

        newThread.interrupt();
        return pass;
    }

    public Runnable getThreadContext() {
        return threadContext;
    }
}
