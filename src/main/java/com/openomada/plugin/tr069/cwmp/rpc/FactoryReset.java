package com.openomada.plugin.tr069.cwmp.rpc;

import com.openomada.plugin.tr069.cwmp.model.CWMPMessage;

public class FactoryReset extends CWMPMessage {
    private static final long serialVersionUID = 1L;

    public FactoryReset() {
        super();
    }

    public FactoryReset(String id) {
        super(id);
    }

    @Override
    public String getRpcName() {
        return "FactoryReset";
    }
}
