package com.openomada.plugin.tr069.cwmp.rpc;

import com.openomada.plugin.tr069.cwmp.model.CWMPMessage;

public class AddObjectResponse extends CWMPMessage {
    private static final long serialVersionUID = 1L;

    private long instanceNumber;
    private int status; // 0=applied, 1=pending

    public AddObjectResponse() {
        super();
    }

    public AddObjectResponse(String id) {
        super(id);
    }

    public AddObjectResponse(String id, long instanceNumber, int status) {
        super(id);
        this.instanceNumber = instanceNumber;
        this.status = status;
    }

    @Override
    public String getRpcName() {
        return "AddObjectResponse";
    }

    public long getInstanceNumber() { return instanceNumber; }
    public void setInstanceNumber(long instanceNumber) { this.instanceNumber = instanceNumber; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
}
