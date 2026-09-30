package com.openomada.plugin.tr069.cwmp.rpc;

import com.openomada.plugin.tr069.cwmp.model.CWMPMessage;

public class DeleteObjectResponse extends CWMPMessage {
    private static final long serialVersionUID = 1L;

    private int status; // 0=applied, 1=pending

    public DeleteObjectResponse() {
        super();
    }

    public DeleteObjectResponse(String id) {
        super(id);
    }

    public DeleteObjectResponse(String id, int status) {
        super(id);
        this.status = status;
    }

    @Override
    public String getRpcName() {
        return "DeleteObjectResponse";
    }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
}
