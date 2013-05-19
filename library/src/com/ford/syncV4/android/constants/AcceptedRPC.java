package com.ford.syncV4.android.constants;

import java.util.ArrayList;

import com.ford.syncV4.proxy.constants.Names;

public class AcceptedRPC {
	ArrayList<String> acceptedRPC = new ArrayList<String>();
	
	public AcceptedRPC() {
		acceptedRPC.add(Names.RegisterAppInterface);
		acceptedRPC.add(Names.UnregisterAppInterface);
		acceptedRPC.add(Names.SetGlobalProperties);
		acceptedRPC.add(Names.ResetGlobalProperties);
		acceptedRPC.add(Names.AddCommand);
		acceptedRPC.add(Names.DeleteCommand);
		acceptedRPC.add(Names.AddSubMenu);
		acceptedRPC.add(Names.DeleteSubMenu);
		acceptedRPC.add(Names.CreateInteractionChoiceSet);
		acceptedRPC.add(Names.PerformInteraction);
		acceptedRPC.add(Names.DeleteInteractionChoiceSet);
		acceptedRPC.add(Names.Alert);
		acceptedRPC.add(Names.Show);
		acceptedRPC.add(Names.Speak);
		acceptedRPC.add(Names.SetMediaClockTimer);
		acceptedRPC.add(Names.EncodedSyncPData);
		acceptedRPC.add(Names.SubscribeButton);
		acceptedRPC.add(Names.UnsubscribeButton);
		
		acceptedRPC.add("ClearMediaClockTimer");
		acceptedRPC.add("PauseMediaClockTimer");
		acceptedRPC.add("ResumeMediaClockTimer");
	}
	
	public String getFunctionName(int i) {
		return acceptedRPC.get(i);
	}
	
	public int getFunctionID(String functionName) {
		return acceptedRPC.indexOf(functionName);
	}
	
	public boolean isAcceptedRPC(String rpc) {
		return acceptedRPC.contains(rpc);
	}
}