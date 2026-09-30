package com.openomada.plugin.tr069.cwmp.rpc;

import com.openomada.plugin.tr069.cwmp.model.CWMPMessage;

public class SetParameterValuesResponse extends CWMPMessage {
    private static final long serialVersionUID = 1L;

    private int status = 0; // 0 = all parameter changes validated and applied; 1 = validated, changes pending

    public SetParameterValuesResponse() {
        super();
    }

    public SetParameterValuesResponse(String id) {
        super(id);
    }

    public SetParameterValuesResponse(String id, int status) {
        super(id);
        this.status = status;
    }

    @Override
    public String getRpcName() {
        return "SetParameterValuesResponse";
    }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
}
