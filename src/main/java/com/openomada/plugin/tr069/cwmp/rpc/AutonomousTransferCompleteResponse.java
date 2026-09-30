package com.openomada.plugin.tr069.cwmp.rpc;

import com.openomada.plugin.tr069.cwmp.model.CWMPMessage;

public class AutonomousTransferCompleteResponse extends CWMPMessage {
    private static final long serialVersionUID = 1L;

    public AutonomousTransferCompleteResponse() {
        super();
    }

    public AutonomousTransferCompleteResponse(String id) {
        super(id);
    }

    @Override
    public String getRpcName() {
        return "AutonomousTransferCompleteResponse";
    }
}
