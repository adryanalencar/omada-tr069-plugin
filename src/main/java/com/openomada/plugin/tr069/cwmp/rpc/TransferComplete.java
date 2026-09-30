package com.openomada.plugin.tr069.cwmp.rpc;

import com.openomada.plugin.tr069.cwmp.model.CWMPMessage;
import com.openomada.plugin.tr069.cwmp.model.FaultStruct;

public class TransferComplete extends CWMPMessage {
    private static final long serialVersionUID = 1L;

    private String commandKey = "";
    private FaultStruct faultStruct;
    private String startTime;
    private String completeTime;

    public TransferComplete() {
        super();
    }

    public TransferComplete(String id) {
        super(id);
    }

    @Override
    public String getRpcName() {
        return "TransferComplete";
    }

    public String getCommandKey() { return commandKey; }
    public void setCommandKey(String commandKey) { this.commandKey = commandKey != null ? commandKey : ""; }

    public FaultStruct getFaultStruct() { return faultStruct; }
    public void setFaultStruct(FaultStruct faultStruct) { this.faultStruct = faultStruct; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getCompleteTime() { return completeTime; }
    public void setCompleteTime(String completeTime) { this.completeTime = completeTime; }
}
