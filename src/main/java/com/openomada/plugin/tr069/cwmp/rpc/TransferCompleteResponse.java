package com.openomada.plugin.tr069.cwmp.rpc;

import com.openomada.plugin.tr069.cwmp.model.CWMPMessage;

public class TransferCompleteResponse extends CWMPMessage {
    private static final long serialVersionUID = 1L;

    public TransferCompleteResponse() {
        super();
    }

    public TransferCompleteResponse(String id) {
        super(id);
    }

    @Override
    public String getRpcName() {
        return "TransferCompleteResponse";
    }
}
