package com.openomada.plugin.tr069.cwmp.rpc;

import com.openomada.plugin.tr069.cwmp.model.CWMPMessage;

public class InformResponse extends CWMPMessage {
    private static final long serialVersionUID = 1L;

    private int maxEnvelopes = 1;

    public InformResponse() {
        super();
    }

    public InformResponse(String id) {
        super(id);
    }

    public InformResponse(String id, int maxEnvelopes) {
        super(id);
        this.maxEnvelopes = maxEnvelopes;
    }

    @Override
    public String getRpcName() {
        return "InformResponse";
    }

    public int getMaxEnvelopes() { return maxEnvelopes; }
    public void setMaxEnvelopes(int maxEnvelopes) { this.maxEnvelopes = maxEnvelopes; }
}
