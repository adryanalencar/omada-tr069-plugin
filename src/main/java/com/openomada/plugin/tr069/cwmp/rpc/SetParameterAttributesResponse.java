package com.openomada.plugin.tr069.cwmp.rpc;

import com.openomada.plugin.tr069.cwmp.model.CWMPMessage;

public class SetParameterAttributesResponse extends CWMPMessage {
    private static final long serialVersionUID = 1L;

    public SetParameterAttributesResponse() {
        super();
    }

    public SetParameterAttributesResponse(String id) {
        super(id);
    }

    @Override
    public String getRpcName() {
        return "SetParameterAttributesResponse";
    }
}
