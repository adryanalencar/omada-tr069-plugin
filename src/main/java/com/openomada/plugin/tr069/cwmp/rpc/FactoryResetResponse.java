package com.openomada.plugin.tr069.cwmp.rpc;

import com.openomada.plugin.tr069.cwmp.model.CWMPMessage;

public class FactoryResetResponse extends CWMPMessage {
    private static final long serialVersionUID = 1L;

    public FactoryResetResponse() {
        super();
    }

    public FactoryResetResponse(String id) {
        super(id);
    }

    @Override
    public String getRpcName() {
        return "FactoryResetResponse";
    }
}
