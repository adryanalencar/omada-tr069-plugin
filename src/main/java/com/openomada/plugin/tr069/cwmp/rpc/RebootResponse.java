package com.openomada.plugin.tr069.cwmp.rpc;

import com.openomada.plugin.tr069.cwmp.model.CWMPMessage;

public class RebootResponse extends CWMPMessage {
    private static final long serialVersionUID = 1L;

    public RebootResponse() {
        super();
    }

    public RebootResponse(String id) {
        super(id);
    }

    @Override
    public String getRpcName() {
        return "RebootResponse";
    }
}
